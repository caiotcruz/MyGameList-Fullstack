package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_follows", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"follower_id", "followed_id"})
})
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower; 

    @ManyToOne
    @JoinColumn(name = "followed_id", nullable = false)
    private User followed; 

    private LocalDateTime followedAt = LocalDateTime.now();
}