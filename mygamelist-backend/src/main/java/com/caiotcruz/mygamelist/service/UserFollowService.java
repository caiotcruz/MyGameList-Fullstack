package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserFollow;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caiotcruz.mygamelist.model.enums.ExperienceSource;
import com.caiotcruz.mygamelist.model.enums.NotificationType;

@Service
public class UserFollowService {
    private final CommunitySafety safety;

    private final UserLocks locks;


    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LevelService levelService;


    public UserFollowService(UserFollowRepository followRepository, UserRepository userRepository, NotificationService notificationService, LevelService levelService, UserLocks locks, CommunitySafety safety) {
        this.safety = safety;
        this.locks = locks;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.levelService = levelService;
    }

    @Transactional
    public void followUser(Long userIdToFollow) {
        User currentUser = getCurrentUser();
        locks.lock(currentUser.getId(), userIdToFollow);
        safety.requireVisible(currentUser.getId(), userIdToFollow);
        User userToFollow = userRepository.findById(userIdToFollow)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Usuário não encontrado"));

        if (currentUser.getId().equals(userToFollow.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Você não pode seguir a si mesmo.");
        }

        if (followRepository.findByFollowerAndFollowed(currentUser, userToFollow).isPresent()) {
            return;
        }

        UserFollow follow = new UserFollow();
        follow.setFollower(currentUser);
        follow.setFollowed(userToFollow);
        followRepository.save(follow);

        levelService.grant(currentUser, ExperienceSource.USER_FOLLOWED);
        levelService.grant(userToFollow, ExperienceSource.USER_GAINED_FOLLOWER);

        notificationService.send(userToFollow, currentUser, NotificationType.FOLLOW, null);
    }

    @Transactional
    public void unfollowUser(Long userIdToUnfollow) {
        User currentUser = getCurrentUser();
        locks.lock(currentUser.getId(), userIdToUnfollow);
        User userToUnfollow = userRepository.findById(userIdToUnfollow)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Usuário não encontrado"));

        UserFollow follow = followRepository.findByFollowerAndFollowed(currentUser, userToUnfollow)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Você não segue este usuário."));

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