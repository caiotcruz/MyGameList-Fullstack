package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    
    // Busca no nosso banco pelo ID da API externa
    Optional<Game> findByRawgId(Long rawgId);
}