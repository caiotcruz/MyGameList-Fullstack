package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.client.RawgClient;
import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.GameHubDTO;
import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.dto.GameReviewDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.ReviewVote;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.model.enums.VoteType;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.GameRepository;
import com.caiotcruz.mygamelist.repository.ReviewVoteRepository;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final UserGameListRepository userGameListRepository; 
    private final RawgClient rawgClient;
    private final ReviewVoteRepository reviewVoteRepository;
    private final UserRepository userRepository;

    @Value("${api.rawg.key}")
    private String apiKey;

    public GameService(GameRepository gameRepository, UserGameListRepository userGameListRepository, RawgClient rawgClient, ReviewVoteRepository reviewVoteRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userGameListRepository = userGameListRepository;
        this.rawgClient = rawgClient;
        this.reviewVoteRepository = reviewVoteRepository;
        this.userRepository = userRepository;
    }

    public List<GameResultDTO> searchGames(String query, Integer page) {
        int pageNumber = (page != null && page > 0) ? page : 1;
        return rawgClient.searchGames(apiKey, query, 16, pageNumber).results();
    }

    public Game getGameContent(Long rawgId) {
        return gameRepository.findByRawgId(rawgId)
                .orElseGet(() -> {
                    log.info("🔍 Jogo não encontrado no DB. Buscando na RAWG API: {}", rawgId);
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

    public void voteOnReview(Long reviewId, Long userId, String voteTypeStr) {
        var review = userGameListRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review não encontrada"));
        
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        VoteType type = VoteType.valueOf(voteTypeStr.toUpperCase());
        
        Optional<ReviewVote> existingVote = reviewVoteRepository.findByUserAndReview(user, review);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            if (vote.getType() == type) {
                reviewVoteRepository.delete(vote);
            } else {
                vote.setType(type);
                reviewVoteRepository.save(vote);
            }
        } else {
            ReviewVote newVote = new ReviewVote(user, review, type);
            reviewVoteRepository.save(newVote);
        }
    }

    public GameHubDTO getGameHubData(Long rawgId, Long currentUserId) {
        Game game = getGameContent(rawgId);
        Long internalId = game.getId();

        GameStats stats = loadStatistics(internalId);
        UserGameEntry myEntry = loadMyEntry(internalId, currentUserId);
        List<GameReviewDTO> reviews = loadReviews(internalId, currentUserId);

        return new GameHubDTO(
                internalId,
                rawgId,
                game.getTitle(),
                game.getCoverUrl(),
                stats.totalPlayers(),
                stats.playingCount(),
                stats.completedCount(),
                stats.platinumCount(),
                stats.avgScore(),
                stats.scoreDistribution(),
                myEntry.status(),
                myEntry.score(),
                myEntry.favorite(),
                reviews
        );
    }

    private GameStats loadStatistics(Long internalId) {
        long totalPlayers = userGameListRepository.countPlayersByGameId(internalId);
        long playingCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.PLAYING);
        long completedCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.COMPLETED);
        long platinumCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.PLATINUM);
        Double avgScore = userGameListRepository.getAverageScoreByGameId(internalId);

        List<Object[]> rawDistribution = userGameListRepository.getScoreDistributionByGameId(internalId);
        Map<Integer, Long> scoreMap = new HashMap<>();

        for (int i = 1; i <= 10; i++) {
            scoreMap.put(i, 0L);
        }

        for (Object[] row : rawDistribution) {
            Integer score = (Integer) row[0];
            Long count = (Long) row[1];
            if (score != null && score >= 1 && score <= 10) {
                scoreMap.put(score, count);
            }
        }
        
        return new GameStats(totalPlayers, playingCount, completedCount, platinumCount, avgScore != null ? avgScore : 0.0, scoreMap);
    }

    private UserGameEntry loadMyEntry(Long internalId, Long currentUserId) {
        if (currentUserId == null) {
            return new UserGameEntry(null, 0, false);
        }

        return userGameListRepository.findByUserIdAndGameId(currentUserId, internalId)
                .map(entry -> new UserGameEntry(entry.getStatus().name(), entry.getScore(), entry.isFavorite()))
                .orElseGet(() -> new UserGameEntry(null, 0, false));
    }

    private List<GameReviewDTO> loadReviews(Long internalId, Long currentUserId) {
        List<UserGameList> reviewEntities = userGameListRepository.findReviewsByGameId(internalId);

        return reviewEntities.stream().map(r -> {
            long likes = reviewVoteRepository.countByReviewAndType(r, VoteType.LIKE);
            long dislikes = reviewVoteRepository.countByReviewAndType(r, VoteType.DISLIKE);
            int karmaScore = (int) ((likes * 2) - dislikes);
            
            String myVote = loadUserVoteForReview(r, currentUserId);

            return new GameReviewDTO(
                r.getId(),
                r.getUser().getName(),
                r.getUser().getProfilePicture(),
                r.getScore(),
                r.getReview(),
                r.getUpdatedAt(),
                likes,
                dislikes,
                karmaScore,
                myVote
            );
        })
        .sorted((r1, r2) -> Integer.compare(r2.voteScore(), r1.voteScore()))
        .limit(10)
        .toList();
    }

    private String loadUserVoteForReview(UserGameList review, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        var userObj = new User(); 
        userObj.setId(currentUserId);
        return reviewVoteRepository.findByUserAndReview(userObj, review)
                .map(vote -> vote.getType().name())
                .orElse(null);
    }

    private record GameStats(
        long totalPlayers, 
        long playingCount, 
        long completedCount, 
        long platinumCount, 
        double avgScore,
        Map<Integer, Long> scoreDistribution
    ) {}

    private record UserGameEntry(
        String status, 
        Integer score, 
        boolean favorite
    ) {}
}