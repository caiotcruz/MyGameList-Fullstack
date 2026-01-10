package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long rawgId; 

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description; 
    
    private String coverUrl; 
    
    private String releaseDate;
}