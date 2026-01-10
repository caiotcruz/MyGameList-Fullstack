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

    // A busca continua sendo pass-through (só repassa)
    public List<GameResultDTO> searchGames(String query, Integer page) {
        int pageNumber = (page != null && page > 0) ? page : 1; // Validação simples
        
        return rawgClient.searchGames(apiKey, query, 16, pageNumber).results();
    }

    // AQUI É A LÓGICA HÍBRIDA
    public Game getGameContent(Long rawgId) {
        // 1. Tenta achar no nosso banco
        return gameRepository.findByRawgId(rawgId)
                .orElseGet(() -> {
                    // 2. Se não achou, vai na API Externa buscar os detalhes
                    System.out.println("🔍 Jogo não encontrado no DB. Buscando na RAWG API: " + rawgId);
                    GameDetailsDTO externalGame = rawgClient.getGameDetails(apiKey, rawgId);

                    // 3. Converte DTO -> Entity
                    Game newGame = new Game();
                    newGame.setRawgId(externalGame.id());
                    newGame.setTitle(externalGame.name());
                    newGame.setDescription(externalGame.description());
                    newGame.setCoverUrl(externalGame.backgroundImage());
                    newGame.setReleaseDate(externalGame.released());

                    // 4. Salva no banco para a próxima vez
                    return gameRepository.save(newGame);
                });
    }
}