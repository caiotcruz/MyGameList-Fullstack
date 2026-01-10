package com.caiotcruz.mygamelist.model;

import com.caiotcruz.mygamelist.model.enums.GameStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_user_game_list", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "game_id"}) // Um usuário não pode adicionar o mesmo jogo 2x
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
    private Game game; // Nosso Game local (que já foi cacheado da RAWG)

    @Enumerated(EnumType.STRING)
    private GameStatus status; // PLAYING, COMPLETED...

    private Integer score; // 0 a 10

    @Column(columnDefinition = "TEXT")
    private String review; // Comentário pessoal

    private LocalDateTime updatedAt = LocalDateTime.now();
}