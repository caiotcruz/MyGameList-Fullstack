package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.ApiResponseDTO;
import com.caiotcruz.mygamelist.dto.ForgotPasswordRequestDTO;
import com.caiotcruz.mygamelist.dto.LoginDTO;
import com.caiotcruz.mygamelist.dto.LoginResponseDTO;
import com.caiotcruz.mygamelist.dto.RegisterDTO;
import com.caiotcruz.mygamelist.dto.ResetPasswordDTO;
import com.caiotcruz.mygamelist.dto.UserSummaryDTO;
import com.caiotcruz.mygamelist.dto.VerificacaoDTO;
import com.caiotcruz.mygamelist.infra.security.TokenService;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.EmailService;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserFollowRepository followRepository;
    private final EmailService emailService;
    private final com.caiotcruz.mygamelist.service.UserLocks locks;

    public AuthController( AuthenticationManager authenticationManager, UserRepository userRepository, TokenService tokenService, UserFollowRepository followRepository, EmailService emailService, com.caiotcruz.mygamelist.service.UserLocks locks) {

        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.followRepository = followRepository;
        this.emailService = emailService;
        this.locks = locks;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginDTO data) {
        var authToken = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(authToken);
        
        User user = (User) auth.getPrincipal(); 
        var token = tokenService.generateToken(user);

        var response = new LoginResponseDTO(token, user.getId(), user.getName());

        return ResponseEntity.ok(new ApiResponseDTO<>(
            "Login realizado com sucesso",
            response
        ));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Void>> register(@RequestBody @Valid RegisterDTO data) {
        if (userRepository.existsByEmail(data.email())) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                "Email já cadastrado.",
                null
            ));
        }

        if (userRepository.existsByName(data.name())) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                "Nome de usuário já está em uso.",
                null
            ));
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));

        User newUser = new User();
        newUser.setEmail(data.email());
        newUser.setName(data.name());
        newUser.setPassword(encryptedPassword);
        newUser.setVerificationCode(code);
        newUser.setVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        newUser.setEnabled(false); 

        userRepository.save(newUser);
        
        emailService.enviarEmailVerificacao(data.email(), code);

        return ResponseEntity.ok(new ApiResponseDTO<>(
            "Código de verificação agendado para envio ao e-mail.",
            null
        ));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponseDTO<Void>> resendVerification(@RequestBody @Valid ForgotPasswordRequestDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user != null) locks.lock(user.getId());
        if (user != null && !user.isEnabled()) {
            String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
            user.setVerificationCode(code);
            user.setVerificationExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            emailService.enviarEmailVerificacao(user.getEmail(), code);
        }
        return ResponseEntity.ok(new ApiResponseDTO<>("Se houver uma conta pendente, um novo código será enviado.", null));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/verify")
    public ResponseEntity<ApiResponseDTO<Void>> verify(@RequestBody @Valid VerificacaoDTO data) {
        UserDetails userDetails = userRepository.findByEmail(data.email());
        
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                "Usuário não encontrado.",
                null
            ));
        }

        User user = (User) userDetails;
        locks.lock(user.getId()); 

        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                "Conta já está ativa.",
                null
            ));
        }

        if (user.getVerificationCode() != null && 
            user.getVerificationCode().equals(data.codigo()) && 
            user.getVerificationExpiry().isAfter(LocalDateTime.now())) {
            
            user.setEnabled(true);
            user.setVerificationCode(null); 
            userRepository.save(user);
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                "Usuário verificado com sucesso",
                null
            ));
        }

        return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
            "Código inexistente ou expirado",
            null
        ));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user != null) locks.lock(user.getId());
        
        if (user != null) {
            String token = java.util.UUID.randomUUID().toString();
            
            user.setResetTokenHash(hashToken(token));
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); 
            userRepository.save(user);
            
            emailService.enviarEmailRecuperacao(user.getEmail(), token);
        }

        return ResponseEntity.ok(new ApiResponseDTO<>("Se o e-mail existir, as instruções foram enviadas.", null));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(@RequestBody @Valid ResetPasswordDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user != null) locks.lock(user.getId());

        if (user != null && user.getResetTokenHash() != null &&
            user.getResetTokenHash().equals(hashToken(data.codigo())) &&
            user.getResetTokenExpiry() != null &&
            user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
            
            String encryptedPassword = new BCryptPasswordEncoder().encode(data.newPassword());
            user.setPassword(encryptedPassword);
            user.setTokenVersion(user.getTokenVersion() + 1);
            user.setResetTokenHash(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponseDTO<>("Senha alterada com sucesso!", null));
        }

        return ResponseEntity.badRequest().body(new ApiResponseDTO<>("Código inválido ou expirado.", null));
    }

    private String hashToken(String token) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}