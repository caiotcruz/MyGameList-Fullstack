package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.dto.CreateCommentDTO;
import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.caiotcruz.mygamelist.model.enums.ExperienceSource;
import com.caiotcruz.mygamelist.model.enums.NotificationType;

import java.util.Optional;

@Service
public class SocialService {

    private final ActivityLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LevelService levelService;

    public SocialService(ActivityLikeRepository likeRepository,
                         CommentRepository commentRepository,
                         ActivityRepository activityRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                        LevelService levelService) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.levelService = levelService;
    }


    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    public boolean toggleLike(Long activityId) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        boolean isSelfActivity = activity.getUser().getId().equals(currentUser.getId());

        Optional<ActivityLike> existingLike = likeRepository.findByUserAndActivity(currentUser, activity);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            if (!isSelfActivity) {
                levelService.revoke(currentUser, ExperienceSource.LIKE_GIVEN);
            }
            return false;
        } else {
            ActivityLike newLike = new ActivityLike(currentUser, activity);
            likeRepository.save(newLike);
            if (!isSelfActivity) {
                levelService.grant(currentUser, ExperienceSource.LIKE_GIVEN);
                notificationService.send(activity.getUser(), currentUser, NotificationType.LIKE, activity);
            }
            return true;
        }
    }


    public CommentDTO addComment(Long activityId, CreateCommentDTO dto) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        boolean isSelfActivity = activity.getUser().getId().equals(currentUser.getId());

        Comment comment = new Comment(dto.text(), currentUser, activity);
        Comment saved = commentRepository.save(comment);

        if (!isSelfActivity) {
            levelService.grant(currentUser, ExperienceSource.COMMENT_POSTED);
            notificationService.send(activity.getUser(), currentUser, NotificationType.COMMENT, activity);
        }

        return CommentDTO.from(saved);
    }

    public void removeComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado"));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Você não tem permissão para remover este comentário.");
        }

        boolean isSelfActivity = comment.getActivity().getUser().getId().equals(currentUser.getId());

        commentRepository.delete(comment);

        if (!isSelfActivity) {
            levelService.revoke(currentUser, ExperienceSource.COMMENT_POSTED);
        }
    }
    
}