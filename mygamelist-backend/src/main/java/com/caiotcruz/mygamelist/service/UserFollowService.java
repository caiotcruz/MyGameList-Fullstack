package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserFollow;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.caiotcruz.mygamelist.model.enums.ExperienceSource;
import com.caiotcruz.mygamelist.model.enums.NotificationType;

@Service
public class UserFollowService {

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LevelService levelService;


    public UserFollowService(UserFollowRepository followRepository, UserRepository userRepository, NotificationService notificationService, LevelService levelService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.levelService = levelService;
    }

    public void followUser(Long userIdToFollow) {
        User currentUser = getCurrentUser();
        User userToFollow = userRepository.findById(userIdToFollow)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (currentUser.getId().equals(userToFollow.getId())) {
            throw new RuntimeException("Você não pode seguir a si mesmo.");
        }

        if (followRepository.findByFollowerAndFollowed(currentUser, userToFollow).isPresent()) {
            throw new RuntimeException("Você já segue este usuário.");
        }

        UserFollow follow = new UserFollow();
        follow.setFollower(currentUser);
        follow.setFollowed(userToFollow);
        followRepository.save(follow);

        levelService.grant(currentUser, ExperienceSource.USER_FOLLOWED);
        levelService.grant(userToFollow, ExperienceSource.USER_GAINED_FOLLOWER);

        notificationService.send(userToFollow, currentUser, NotificationType.FOLLOW, null);
    }

    public void unfollowUser(Long userIdToUnfollow) {
        User currentUser = getCurrentUser();
        User userToUnfollow = userRepository.findById(userIdToUnfollow)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UserFollow follow = followRepository.findByFollowerAndFollowed(currentUser, userToUnfollow)
                .orElseThrow(() -> new RuntimeException("Você não segue este usuário."));

        followRepository.delete(follow);

        levelService.revoke(currentUser, ExperienceSource.USER_FOLLOWED);
        levelService.revoke(userToUnfollow, ExperienceSource.USER_GAINED_FOLLOWER);
    }
    
    public boolean isFollowing(Long userId) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) return false;
        
        return followRepository.findByFollowerAndFollowed(currentUser, targetUser).isPresent();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }
}