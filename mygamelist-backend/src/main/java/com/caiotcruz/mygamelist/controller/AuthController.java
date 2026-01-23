package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.LoginDTO;
import com.caiotcruz.mygamelist.dto.LoginResponseDTO;
import com.caiotcruz.mygamelist.dto.RegisterDTO;
import com.caiotcruz.mygamelist.dto.UserSummaryDTO;
import com.caiotcruz.mygamelist.infra.security.TokenService;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        
        User user = (User) auth.getPrincipal(); 
        var token = tokenService.generateToken(user);
        
        return ResponseEntity.ok(new LoginResponseDTO(token, user.getId(), user.getName()));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data) {
        if (userRepository.findByEmail(data.email()) != null) return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        
        User newUser = new User();
        newUser.setEmail(data.email());
        newUser.setName(data.name());
        newUser.setPassword(encryptedPassword);

        userRepository.save(newUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<UserSummaryDTO> getAllUsers() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(u -> !u.getId().equals(currentUser.getId())) 
                .map(u -> {
                    boolean isFollowing = followRepository.findByFollowerAndFollowed(currentUser, u).isPresent();
                    return new UserSummaryDTO(u.getId(), u.getName(), isFollowing);
                })
                .collect(Collectors.toList());
    }
}