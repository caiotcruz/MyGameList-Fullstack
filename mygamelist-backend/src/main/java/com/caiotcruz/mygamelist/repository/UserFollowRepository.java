package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollowerAndFollowed(User follower, User followed);
    
    long countByFollowed(User user);
    
    long countByFollower(User user);
}