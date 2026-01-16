package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.caiotcruz.mygamelist.model.enums.NotificationType;

@Entity
@Table(name = "tb_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem recebe a notificação (ex: Eu)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Quem provocou a notificação (ex: Meu Amigo)
    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    // Tipo de notificação
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // Referência opcional à atividade (para saber qual jogo foi)
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

    // Getters e Setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}