package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.model.enums.NotificationType;
import com.caiotcruz.mygamelist.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // Método genérico para enviar notificação
    public void send(User recipient, User actor, NotificationType type, Activity activity) {
        // Regra de Ouro: Não notificar se eu curtir meu próprio post
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = new Notification(recipient, actor, type, activity);
        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long countUnread(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
    
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}