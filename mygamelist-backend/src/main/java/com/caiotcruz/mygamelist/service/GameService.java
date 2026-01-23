package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.client.RawgClient;
import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.GameHubDTO;
import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.dto.GameReviewDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.GameRepository;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserGameListRepository userGameListRepository; 

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

    public GameHubDTO getGameHubData(Long rawgId, Long currentUserId) {
        
        Game game = getGameContent(rawgId);
        Long internalId = game.getId();

        long totalPlayers = userGameListRepository.countPlayersByGameId(internalId);
        long playingCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.PLAYING);
        long completedCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.COMPLETED);
        Double avgScore = userGameListRepository.getAverageScoreByGameId(internalId);

        String myStatus = null;
        Integer myScore = 0;
        boolean myFavorite = false;
        List<GameReviewDTO> reviews = userGameListRepository.findLatestReviews(internalId);

        if (currentUserId != null) {
            Optional<UserGameList> myEntry = userGameListRepository.findByUserIdAndGameId(currentUserId, internalId);
            if (myEntry.isPresent()) {
                myStatus = myEntry.get().getStatus().name();
                myScore = myEntry.get().getScore();
                myFavorite = myEntry.get().isFavorite();
            }
        }

        return new GameHubDTO(
                internalId,
                rawgId,
                game.getTitle(),
                game.getCoverUrl(),
                totalPlayers,
                playingCount,
                completedCount,
                avgScore != null ? avgScore : 0.0,
                myStatus,
                myScore,
                myFavorite,
                reviews
        );
    }
}