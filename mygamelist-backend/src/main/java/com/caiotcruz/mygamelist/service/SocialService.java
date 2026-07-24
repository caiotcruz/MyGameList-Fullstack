package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.dto.CreateCommentDTO;
import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.caiotcruz.mygamelist.model.enums.NotificationType;

import java.util.Optional;

@Service
public class SocialService {

    private final ActivityLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SocialService(ActivityLikeRepository likeRepository,
                         CommentRepository commentRepository,
                         ActivityRepository activityRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }


    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    public boolean toggleLike(Long activityId) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        Optional<ActivityLike> existingLike = likeRepository.findByUserAndActivity(currentUser, activity);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return false; 
        } else {
            ActivityLike newLike = new ActivityLike(currentUser, activity);
            likeRepository.save(newLike);
            notificationService.send(activity.getUser(), currentUser, NotificationType.LIKE, activity);
            return true; 
        }
    }

    public Comment addComment(Long activityId, CommentDTO dto) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        Comment comment = new Comment(dto.text(), currentUser, activity);
        notificationService.send(activity.getUser(), currentUser, NotificationType.COMMENT, activity);
        return commentRepository.save(comment);
    }

    public CommentDTO addComment(Long activityId, CreateCommentDTO dto) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        Comment comment = new Comment(dto.text(), currentUser, activity);
        Comment saved = commentRepository.save(comment);
        notificationService.send(activity.getUser(), currentUser, NotificationType.COMMENT, activity);
        return CommentDTO.from(saved);
    }
}