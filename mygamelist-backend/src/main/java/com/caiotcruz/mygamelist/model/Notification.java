package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

import com.caiotcruz.mygamelist.model.enums.NotificationType;

@Data
@Entity
@Table(name = "tb_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;

    private boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Notification(User user, User actor, NotificationType type, Activity activity) {
        this.user = user;
        this.actor = actor;
        this.type = type;
        this.activity = activity;
    }
}