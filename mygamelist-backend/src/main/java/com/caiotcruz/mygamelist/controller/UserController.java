package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.UserProfileDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(@RequestBody UserProfileDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        User user = (User) userRepository.findByEmail(email);


        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        if (dto.bio() != null) {
            user.setBio(dto.bio());
        }

        if (dto.profilePicture() != null) {
            user.setProfilePicture(dto.profilePicture());
        }

        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(updatedUser);
    }
}