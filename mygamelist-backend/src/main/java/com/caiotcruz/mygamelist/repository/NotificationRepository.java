package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Notification;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    java.util.Optional<Notification> findByIdAndUser(Long id, User user);
    
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);
}