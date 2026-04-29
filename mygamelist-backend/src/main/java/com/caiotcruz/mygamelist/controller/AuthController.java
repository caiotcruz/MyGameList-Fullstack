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
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private UserFollowRepository followRepository;

    @Autowired
    private EmailService emailService;

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
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));

        User newUser = new User();
        newUser.setEmail(data.email());
        newUser.setName(data.name());
        newUser.setPassword(encryptedPassword);
        newUser.setVerificationCode(code);
        newUser.setVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        newUser.setEnabled(false); 

        userRepository.save(newUser);
        
        try {
            emailService.enviarEmailVerificacao(data.email(), code);
        } catch (Exception e) {
            System.out.println("ERRO AO ENVIAR EMAIL: " + e.getMessage());
        }

        return ResponseEntity.ok(new ApiResponseDTO<>(
            "Código de verificação enviado ao e-mail.",
            null
        ));
    }

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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        
        if (user != null) {
            // Geramos um token longo e único
            String token = java.util.UUID.randomUUID().toString();
            
            user.setVerificationCode(token); // Reutilizamos a coluna, mas agora com o UUID
            user.setVerificationExpiry(LocalDateTime.now().plusHours(1)); // Tokens de link costumam durar mais (ex: 1h)
            userRepository.save(user);
            
            try {
                emailService.enviarEmailRecuperacao(user.getEmail(), token);
            } catch (Exception e) {
                System.out.println("Erro e-mail: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(new ApiResponseDTO<>("Se o e-mail existir, as instruções foram enviadas.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(@RequestBody @Valid ResetPasswordDTO data) {
        User user = (User) userRepository.findByEmail(data.email());

        if (user != null && user.getVerificationCode() != null &&
            user.getVerificationCode().equals(data.codigo()) &&
            user.getVerificationExpiry().isAfter(LocalDateTime.now())) {
            
            String encryptedPassword = new BCryptPasswordEncoder().encode(data.newPassword());
            user.setPassword(encryptedPassword);
            user.setVerificationCode(null); // Limpa o código
            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponseDTO<>("Senha alterada com sucesso!", null));
        }

        return ResponseEntity.badRequest().body(new ApiResponseDTO<>("Código inválido ou expirado.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<UserSummaryDTO>>> getAllUsers() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<UserSummaryDTO> users = userRepository.findAll().stream()
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .map(u -> {
                boolean isFollowing = followRepository
                    .findByFollowerAndFollowed(currentUser, u)
                    .isPresent();

                // Agora passando o 4º parâmetro: imageUrl
                return new UserSummaryDTO(u.getId(), u.getName(), isFollowing, u.getProfilePicture());
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseDTO<>("Usuários carregados com sucesso", users));
    }
}