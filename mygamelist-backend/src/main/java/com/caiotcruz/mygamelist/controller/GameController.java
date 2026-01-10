package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping("/search")
    public List<GameResultDTO> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") Integer page // <--- NOVO (Padrão 1)
    ) {
        return gameService.searchGames(query, page);
    }

    // NOVO ENDPOINT: Pega detalhes (e salva silenciosamente se precisar)
    @GetMapping("/{id}")
    public Game getGameDetails(@PathVariable Long id) {
        return gameService.getGameContent(id);
    }
}