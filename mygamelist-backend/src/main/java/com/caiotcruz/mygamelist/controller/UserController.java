package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.UserProfileDTO;
import com.caiotcruz.mygamelist.dto.PublicUserDTO;
import jakarta.validation.Valid;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    private final com.caiotcruz.mygamelist.service.UserLocks locks;

    private final com.caiotcruz.mygamelist.service.CommunitySafety safety;


    private final UserRepository userRepository;

    public UserController(UserRepository userRepository, com.caiotcruz.mygamelist.service.CommunitySafety safety, com.caiotcruz.mygamelist.service.UserLocks locks) {
        this.locks = locks;
        this.safety = safety;
        this.userRepository = userRepository;
    }
    @GetMapping("/{id}")
    public ResponseEntity<PublicUserDTO> getUserById(@PathVariable Long id) {
        safety.requireProfile(id);
        Optional<User> user = userRepository.findById(id);

        return user.map(PublicUserDTO::from).map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.transaction.annotation.Transactional
    @PutMapping("/me")
    public ResponseEntity<PublicUserDTO> updateMyProfile(@Valid @RequestBody UserProfileDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = (User) userRepository.findByEmail(email);
        locks.lock(user.getId());


        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        if (dto.theme() != null) user.setTheme(dto.theme());

        if (dto.bio() != null) {
            user.setBio(dto.bio());
        }

        if (dto.rotatingAvatar() != null) {
            user.setRotatingAvatar(dto.rotatingAvatar());
        }

        if (dto.profilePicture() != null) {
            String picture = dto.profilePicture().trim();
            if (!picture.isEmpty() && !(picture.startsWith("https://") || picture.startsWith("data:image/png;base64,")
                    || picture.startsWith("data:image/jpeg;base64,") || picture.startsWith("data:image/webp;base64,"))) {
                throw new IllegalArgumentException("URL de avatar inválida");
            }
            user.setProfilePicture(picture);
        }

        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(PublicUserDTO.from(updatedUser));
    }
}
