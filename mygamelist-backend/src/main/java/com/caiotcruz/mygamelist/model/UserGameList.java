package com.caiotcruz.mygamelist.model;

import com.caiotcruz.mygamelist.model.enums.GameStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_user_game_list", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "game_id"})
})
public class UserGameList {

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
    private GameStatus status; 

    private Integer score; 

    @Column(columnDefinition = "TEXT")
    private String review; 

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}