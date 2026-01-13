package com.caiotcruz.mygamelist.model;

import com.caiotcruz.mygamelist.model.enums.ActivityType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "tb_activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    private ActivityType type;

    private String detail; 

    private LocalDateTime timestamp = LocalDateTime.now();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("activity") // Evita loop infinito no JSON (Activity -> Like -> Activity...)
    private List<ActivityLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("activity") // Evita loop infinito
    private List<Comment> comments = new ArrayList<>();

    // 👇 GETTERS E SETTERS
    public List<ActivityLike> getLikes() {
        return likes;
    }

    public void setLikes(List<ActivityLike> likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}