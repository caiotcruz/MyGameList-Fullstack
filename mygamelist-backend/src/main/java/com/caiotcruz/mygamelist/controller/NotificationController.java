package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.model.Notification;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    @GetMapping
    public List<Notification> getMyNotifications() {
        return notificationService.getUserNotifications(getCurrentUser());
    }

    @GetMapping("/unread-count")
    public long getUnreadCount() {
        return notificationService.countUnread(getCurrentUser());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
    
    @PutMapping("/read-all")
    public void markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUser());
    }
}