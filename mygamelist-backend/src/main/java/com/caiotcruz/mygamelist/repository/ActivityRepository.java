package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    @Query("SELECT a FROM Activity a WHERE a.user IN (SELECT f.followed FROM UserFollow f WHERE f.follower = :user) ORDER BY a.timestamp DESC")
    List<Activity> findFeedByFollower(@Param("user") User user);

    List<Activity> findAllByOrderByTimestampDesc();
}