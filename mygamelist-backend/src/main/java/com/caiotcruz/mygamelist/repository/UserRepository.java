package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.User;

import feign.Param;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, Long> {
    UserDetails findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByName(String name);
    
    List<User> findByNameContainingIgnoreCase(String name);

   @Query("""
        SELECT DISTINCT u, COUNT(f2) as mutualCount 
        FROM User u 
        JOIN UserFollow f1 ON f1.followed = u 
        JOIN UserFollow f2 ON f2.followed = f1.follower 
        WHERE f2.follower = :currentUser 
        AND u <> :currentUser 
        AND u NOT IN (
            SELECT f3.followed 
            FROM UserFollow f3 
            WHERE f3.follower = :currentUser
        )
        GROUP BY u 
        ORDER BY mutualCount DESC
    """)
    List<Object[]> findSuggestionsWithMutualCount(@Param("currentUser") User currentUser);

}