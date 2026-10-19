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

    private final com.caiotcruz.mygamelist.infra.security.PasswordWork passwordWork;
    private final com.caiotcruz.mygamelist.infra.security.VerificationCodes codes;
    private final com.caiotcruz.mygamelist.infra.security.SecurityEvents events;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserFollowRepository followRepository;
    private final EmailService emailService;
    private final com.caiotcruz.mygamelist.service.UserLocks locks;

    public AuthController( AuthenticationManager authenticationManager, UserRepository userRepository, TokenService tokenService, UserFollowRepository followRepository, EmailService emailService, com.caiotcruz.mygamelist.service.UserLocks locks,
            com.caiotcruz.mygamelist.infra.security.PasswordWork passwordWork,
            com.caiotcruz.mygamelist.infra.security.VerificationCodes codes,
            com.caiotcruz.mygamelist.infra.security.SecurityEvents events) {
        this.passwordWork=passwordWork;this.codes=codes;this.events=events;

        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.followRepository = followRepository;
        this.emailService = emailService;
        this.locks = locks;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginDTO data) {
        var auth = passwordWork.authenticate(authenticationManager,data.email(),data.password());

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
        String encryptedPassword = passwordWork.encode(data.password());
        if (userRepository.existsByEmail(data.email())) return registrationAccepted();

        if (userRepository.existsByName(data.name())) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                "Nome de usuário já está em uso.",
                null
            ));
        }


        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));

        User newUser = new User();
        newUser.setEmail(data.email());
        newUser.setName(data.name());
        newUser.setPassword(encryptedPassword);
        newUser.setVerificationCode(codes.hash(data.email(),code));
        newUser.setVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        newUser.setEnabled(false);

        userRepository.save(newUser);

        emailService.enviarEmailVerificacao(data.email(), code);

        events.record("registration_queued");
        return registrationAccepted();
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponseDTO<Void>> resendVerification(@RequestBody @Valid ForgotPasswordRequestDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user != null) locks.lock(user.getId());
        if (user != null && !user.isEnabled()) {
            String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
            user.setVerificationCode(codes.hash(user.getEmail(),code));
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
                "Código inválido ou expirado.",
                null
            ));
        }

        User user = (User) userDetails;
        locks.lock(user.getId());

        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                "Código inválido ou expirado.",
                null
            ));
        }

        if (user.getVerificationCode() != null &&
            codes.matches(user.getEmail(),data.codigo(),user.getVerificationCode()) &&
            user.getVerificationExpiry()!=null &&
            user.getVerificationExpiry().isAfter(LocalDateTime.now())) {

            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            events.record("email_verified");
            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponseDTO<>(
                "Usuário verificado com sucesso",
                null
            ));
        }

        return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
            "Código inválido ou expirado.",
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

            String encryptedPassword = passwordWork.encode(data.newPassword());
            user.setPassword(encryptedPassword);
            user.setTokenVersion(user.getTokenVersion() + 1);
            events.record("password_reset");
            user.setResetTokenHash(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponseDTO<>("Senha alterada com sucesso!", null));
        }

        return ResponseEntity.badRequest().body(new ApiResponseDTO<>("Código inválido ou expirado.", null));
    }

    private ResponseEntity<ApiResponseDTO<Void>> registrationAccepted(){
        return ResponseEntity.ok(new ApiResponseDTO<>("Se o cadastro puder ser concluído, você receberá as instruções por e-mail.",null));
    }

    @PostMapping("/logout-all")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponseDTO<Void>> logoutAll(){
        User user=(User)userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        locks.lock(user.getId());user.setTokenVersion(user.getTokenVersion()+1);userRepository.save(user);
        events.record("sessions_revoked");
        return ResponseEntity.ok(new ApiResponseDTO<>("Sessões encerradas.",null));
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
