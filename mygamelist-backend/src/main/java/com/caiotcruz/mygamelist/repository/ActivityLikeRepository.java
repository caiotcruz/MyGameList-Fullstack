package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.ActivityLike;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityLikeRepository extends JpaRepository<ActivityLike, Long> {
    Optional<ActivityLike> findByUserAndActivity(User user, Activity activity);
}