package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Notification;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Busca notificações do usuário, ordenadas da mais nova para mais antiga
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Conta quantas não lidas existem (para a bolinha vermelha)
    long countByUserAndIsReadFalse(User user);
}