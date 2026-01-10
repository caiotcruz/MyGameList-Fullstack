package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.client.RawgClient;
import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RawgClient rawgClient;

    @Value("${api.rawg.key}")
    private String apiKey;

    public List<GameResultDTO> searchGames(String query, Integer page) {
        int pageNumber = (page != null && page > 0) ? page : 1; 
        
        return rawgClient.searchGames(apiKey, query, 16, pageNumber).results();
    }

    public Game getGameContent(Long rawgId) {
        return gameRepository.findByRawgId(rawgId)
                .orElseGet(() -> {
                    System.out.println("🔍 Jogo não encontrado no DB. Buscando na RAWG API: " + rawgId);
                    GameDetailsDTO externalGame = rawgClient.getGameDetails(apiKey, rawgId);

                    Game newGame = new Game();
                    newGame.setRawgId(externalGame.id());
                    newGame.setTitle(externalGame.name());
                    newGame.setDescription(externalGame.description());
                    newGame.setCoverUrl(externalGame.backgroundImage());
                    newGame.setReleaseDate(externalGame.released());

                    return gameRepository.save(newGame);
                });
    }
}