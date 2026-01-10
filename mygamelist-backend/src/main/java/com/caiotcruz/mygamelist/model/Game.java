package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Nosso ID interno (1, 2, 3...)

    @Column(unique = true, nullable = false)
    private Long rawgId; // O ID original lá na RAWG (ex: 3498 para God of War)

    private String title;
    
    @Column(columnDefinition = "TEXT") // Descrições podem ser longas
    private String description; 
    
    private String coverUrl; // URL da imagem
    
    private String releaseDate; // Data de lançamento (String simples por enquanto)
}