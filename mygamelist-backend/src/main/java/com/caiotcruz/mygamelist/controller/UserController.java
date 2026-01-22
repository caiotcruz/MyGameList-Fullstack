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

    // 👇 MÉTODO NOVO: Necessário para o carregarPerfil() do Frontend funcionar
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        
        // Se achar o usuário, retorna. Se não, 404.
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(@RequestBody UserProfileDTO dto) {
        // Pega o usuário logado via Token
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Garante que estamos pegando a entidade gerenciada pelo banco
        User user = (User) userRepository.findByEmail(email);

        // --- ATUALIZAÇÃO SEGURA (Só atualiza se o dado foi enviado) ---

        // 1. Nome
        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        // 2. Bio (Só altera se não for nulo)
        if (dto.bio() != null) {
            user.setBio(dto.bio());
        }

        // 3. Foto (Só altera se não for nulo)
        if (dto.profilePicture() != null) {
            user.setProfilePicture(dto.profilePicture());
        }

        // Salva no banco
        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(updatedUser);
    }
}