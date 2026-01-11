package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findAllByOrderByTimestampDesc();
}