package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.caiotcruz.mygamelist.model.enums.NotificationType;

import java.util.Optional;

@Service
public class SocialService {

    @Autowired
    private ActivityLikeRepository likeRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationService notificationService;

    // Método auxiliar para pegar o usuário logado
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    // Lógica do Like (Toggle: Curte ou Descurte)
    public boolean toggleLike(Long activityId) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        Optional<ActivityLike> existingLike = likeRepository.findByUserAndActivity(currentUser, activity);

        if (existingLike.isPresent()) {
            // Se já existe, remove (Descurtir)
            likeRepository.delete(existingLike.get());
            return false; // Retorna false indicando que agora NÃO está curtido
        } else {
            // Se não existe, cria (Curtir)
            ActivityLike newLike = new ActivityLike(currentUser, activity);
            likeRepository.save(newLike);
            notificationService.send(activity.getUser(), currentUser, NotificationType.LIKE, activity);
            return true; // Retorna true indicando que agora ESTÁ curtido
        }
    }

    // Lógica de Comentar
    public Comment addComment(Long activityId, CommentDTO dto) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        Comment comment = new Comment(dto.text(), currentUser, activity);
        notificationService.send(activity.getUser(), currentUser, NotificationType.COMMENT, activity);
        return commentRepository.save(comment);
    }
}