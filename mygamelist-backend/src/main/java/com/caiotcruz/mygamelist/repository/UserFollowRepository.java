package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.dto.MutualFriendDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserFollow;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollowerAndFollowed(User follower, User followed);
    
    long countByFollowed(User user);
    
    long countByFollower(User user);

    @Query("""
        SELECT new com.caiotcruz.mygamelist.dto.MutualFriendDTO(
            f1.followed.id,
            f1.followed.name,
            f1.followed.profilePicture
        )
        FROM UserFollow f1
        JOIN UserFollow f2
            ON f1.followed = f2.follower
        WHERE f1.follower = :currentUser
        AND f2.followed = :suggestedUser
    """)
    List<MutualFriendDTO> findMutualFollowers(
            @Param("currentUser") User currentUser,
            @Param("suggestedUser") User suggestedUser
    );
}