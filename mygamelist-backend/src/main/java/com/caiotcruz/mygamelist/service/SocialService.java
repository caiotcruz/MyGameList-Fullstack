package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.dto.CreateCommentDTO;
import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caiotcruz.mygamelist.model.enums.ExperienceSource;
import com.caiotcruz.mygamelist.model.enums.NotificationType;

import java.util.Optional;

@Service
public class SocialService {
    private final CommunitySafety safety;

    private final UserLocks locks;


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
                        LevelService levelService, UserLocks locks, CommunitySafety safety) {
        this.safety = safety;
        this.locks = locks;
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

    @Transactional
    public boolean toggleLike(Long activityId) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Atividade não encontrada"));

        locks.lock(currentUser.getId(), activity.getUser().getId());
        safety.requireVisible(currentUser.getId(), activity.getUser().getId());
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


    @Transactional
    public CommentDTO addComment(Long activityId, CreateCommentDTO dto) {
        User currentUser = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Atividade não encontrada"));

        locks.lock(currentUser.getId(), activity.getUser().getId());
        safety.requireVisible(currentUser.getId(), activity.getUser().getId());
        boolean isSelfActivity = activity.getUser().getId().equals(currentUser.getId());

        Comment comment = new Comment(dto.text(), currentUser, activity);
        Comment saved = commentRepository.save(comment);

        if (!isSelfActivity) {
            levelService.grant(currentUser, ExperienceSource.COMMENT_POSTED);
            notificationService.send(activity.getUser(), currentUser, NotificationType.COMMENT, activity);
        }

        return CommentDTO.from(saved);
    }

    @Transactional
    public void removeComment(Long activityId, Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Comentário não encontrado"));

        if (!comment.getActivity().getId().equals(activityId)) {
            throw new IllegalArgumentException("Comentário não pertence à atividade.");
        }
        locks.lock(comment.getUser().getId(), comment.getActivity().getUser().getId());
        if (!commentRepository.existsById(commentId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Comentário já removido.");
        boolean isAuthor = comment.getUser().getId().equals(currentUser.getId());
        boolean isActivityOwner = comment.getActivity().getUser().getId().equals(currentUser.getId());
        if (!isAuthor && !isActivityOwner) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Você não tem permissão para remover este comentário.");
        }

        boolean isSelfActivity = comment.getActivity().getUser().getId().equals(comment.getUser().getId());

        commentRepository.delete(comment);

        if (!isSelfActivity) {
            levelService.revoke(comment.getUser(), ExperienceSource.COMMENT_POSTED);
        }
    }
    
}