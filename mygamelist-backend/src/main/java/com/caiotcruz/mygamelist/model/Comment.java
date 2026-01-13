package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT") // Permite textos longos
    private String text;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Quem comentou?
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Comentou em qual atividade?
    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;

    // Construtor vazio (obrigatório pro Hibernate)
    public Comment() {}

    // Construtor prático
    public Comment(String text, User user, Activity activity) {
        this.text = text;
        this.user = user;
        this.activity = activity;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}