package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.model.enums.NotificationType;
import com.caiotcruz.mygamelist.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final CommunitySafety safety;


    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository, CommunitySafety safety) {
        this.safety = safety;
        this.notificationRepository = notificationRepository;
    }

    public void send(User recipient, User actor, NotificationType type, Activity activity) {
        if (recipient.getId().equals(actor.getId()) || safety.blocked(recipient.getId(),actor.getId())) {
            return;
        }

        Notification notification = new Notification(recipient, actor, type, activity);
        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(User user) {
        var hidden=safety.hiddenIds(user.getId());
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream().filter(n -> n.getActor()==null || !hidden.contains(n.getActor().getId())).toList();
    }

    public long countUnread(User user) {
        return getUserNotifications(user).stream().filter(n -> !n.isRead()).count();
    }

    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findByIdAndUser(notificationId, user).ifPresent(n -> {
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