package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.NotificationDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.NotificationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    @GetMapping
    public List<NotificationDTO> getMyNotifications() {
        return notificationService.getUserNotifications(getCurrentUser()).stream().map(NotificationDTO::from).toList();
    }

    @GetMapping("/unread-count")
    public long getUnreadCount() {
        return notificationService.countUnread(getCurrentUser());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, getCurrentUser());
    }
    
    @PutMapping("/read-all")
    public void markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUser());
    }
}