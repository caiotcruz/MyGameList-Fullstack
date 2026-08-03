package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.client.RawgClient;
import com.caiotcruz.mygamelist.dto.GameAchievementDTO;
import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.GameHubDTO;
import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.dto.GameReviewDTO;
import com.caiotcruz.mygamelist.dto.GameScreenshotDTO;
import com.caiotcruz.mygamelist.dto.GameStoreDTO;
import com.caiotcruz.mygamelist.dto.GameTrailerDTO;
import com.caiotcruz.mygamelist.dto.RelatedGameDTO;
import com.caiotcruz.mygamelist.dto.TrendingGameDTO;
import com.caiotcruz.mygamelist.dto.rawg.RawgAchievementDTO;
import com.caiotcruz.mygamelist.dto.rawg.RawgAchievementsResponse;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.ReviewVote;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.enums.ActivityType;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.model.enums.VoteType;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import com.caiotcruz.mygamelist.repository.GameRepository;
import com.caiotcruz.mygamelist.repository.ReviewVoteRepository;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ActivityRepository activityRepository; 

    @Value("${api.rawg.key}")
    private String apiKey;

    public GameService(GameRepository gameRepository, UserGameListRepository userGameListRepository, RawgClient rawgClient, ReviewVoteRepository reviewVoteRepository, UserRepository userRepository, ActivityRepository activityRepository) {
        this.gameRepository = gameRepository;
        this.userGameListRepository = userGameListRepository;
        this.rawgClient = rawgClient;
        this.reviewVoteRepository = reviewVoteRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
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
                    newGame.setMetacritic(externalGame.metacritic());

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
                game.getMetacritic(),
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

    public List<RelatedGameDTO> getAdditions(Long rawgId) {
        try {
            return rawgClient.getAdditions(apiKey, rawgId).results().stream()
                    .map(g -> new RelatedGameDTO(g.id(), g.name(), g.backgroundImage(), g.released(), g.metacritic()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar additions da RAWG para o jogo {}: {}", rawgId, e.getMessage());
            return List.of();
        }
    }

    public List<RelatedGameDTO> getGameSeries(Long rawgId) {
        try {
            return rawgClient.getGameSeries(apiKey, rawgId).results().stream()
                    .map(g -> new RelatedGameDTO(g.id(), g.name(), g.backgroundImage(), g.released(), g.metacritic()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar game-series da RAWG para o jogo {}: {}", rawgId, e.getMessage());
            return List.of();
        }
    }

    public List<GameScreenshotDTO> getScreenshots(Long rawgId) {
        try {
            return rawgClient.getScreenshots(apiKey, rawgId).results().stream()
                    .map(s -> new GameScreenshotDTO(s.id(), s.image()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar screenshots da RAWG para o jogo {}: {}", rawgId, e.getMessage());
            return List.of();
        }
    }

    public List<GameStoreDTO> getStores(Long rawgId) {
        try {
            return rawgClient.getStores(apiKey, rawgId).results().stream()
                    .map(s -> new GameStoreDTO(s.storeId(), RawgStoreCatalog.nameFor(s.storeId()), s.url()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar stores da RAWG para o jogo {}: {}", rawgId, e.getMessage());
            return List.of();
        }
    }

    public List<GameAchievementDTO> getAchievements(Long rawgId) {
        List<GameAchievementDTO> all = new ArrayList<>();
        int page = 1;
        int pageSize = 40;
        int maxPages = 10;

        try {
            while (page <= maxPages) {
                RawgAchievementsResponse response = rawgClient.getAchievements(apiKey, rawgId, page, pageSize);
                List<RawgAchievementDTO> results = response.results();

                all.addAll(results.stream()
                        .map(a -> new GameAchievementDTO(a.id(), a.name(), a.description(), a.image(), a.percent()))
                        .toList());

                boolean isLastPage = response.next() == null || results.isEmpty();
                if (isLastPage) break;

                page++;
            }
        } catch (Exception e) {
            log.warn("Falha ao buscar achievements da RAWG para o jogo {}: {}", rawgId, e.getMessage());
        }

        return all;
    }

    public List<GameTrailerDTO> getTrailers(Long rawgId) {
        try {
            return rawgClient.getMovies(apiKey, rawgId).results().stream()
                    .map(m -> new GameTrailerDTO(
                            m.id(),
                            m.name(),
                            m.preview(),
                            m.data().max() != null ? m.data().max() : m.data().p480()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar trailers da RAWG para o jogo {}: {}", rawgId, e.getMessage());
            return List.of();
        }
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
                r.getUser().getId(),
                r.getUser().getProfilePicture(),
                r.getScore(),
                r.getReview(),
                r.isSpoiler(),
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

    public List<TrendingGameDTO> getTrendingGames(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<ActivityType> relevantTypes = List.of(ActivityType.ADDED_TO_LIST, ActivityType.REVIEWED);

        List<TrendingGameDTO> trendingList = new java.util.ArrayList<>(
                activityRepository.findTrendingGames(
                        relevantTypes,
                        startOfMonth,
                        PageRequest.of(0, safeLimit)
                )
        );

        if (trendingList.size() >= safeLimit) {
            return trendingList;
        }

        List<TrendingGameDTO> fallbackGames = List.of(
            new TrendingGameDTO(null, 730L, "Counter-Strike 2", "https://media.rawg.io/media/games/736/736192801c692b8941c23be6e0236164.jpg", 980),
            new TrendingGameDTO(null, 5214L, "Minecraft", "https://media.rawg.io/media/games/14a/14a83c56ff292b7ae6a878892d3010ca.jpg", 930),
            new TrendingGameDTO(null, 22511L, "Fortnite", "https://media.rawg.io/media/games/2d9/2d9a1e6ddc5f0f2b8f8c88e6b8b6b5d5.jpg", 910),
            new TrendingGameDTO(null, 2572L, "League of Legends", "https://media.rawg.io/media/games/78b/78bc81e247fc7e7494ea2e882b5f450c.jpg", 890),
            new TrendingGameDTO(null, 28121L, "VALORANT", "https://media.rawg.io/media/games/b72/b7233d7286622c77d036b1a78ee5770b.jpg", 860),
            new TrendingGameDTO(null, 12020L, "Dota 2", "https://media.rawg.io/media/games/99b/99b9a5f6ef8aeecc4430ebdc80bd5139.jpg", 820),
            new TrendingGameDTO(null, 3498L, "Grand Theft Auto V", "https://media.rawg.io/media/games/20a/20aa03a10cdaef09362d297b0a88e8bf.jpg", 1000),
            new TrendingGameDTO(null, 274755L, "Marvel Rivals", "https://media.rawg.io/media/games/placeholder.jpg", 760),
            new TrendingGameDTO(null, 11973L, "Apex Legends", "https://media.rawg.io/media/games/ea5/ea5f9e4b0b91cb3cbf1ebd7af5120a6d.jpg", 720),
            new TrendingGameDTO(null, 278L, "Rocket League", "https://media.rawg.io/media/games/40d/40d5b37c6e5d0f0ebd8c4d7f82f6d01d.jpg", 680)
        );

        for (TrendingGameDTO fallback : fallbackGames) {
            boolean jaExisteNaLista = trendingList.stream()
                    .anyMatch(item -> item.rawgId().equals(fallback.rawgId()));

            if (!jaExisteNaLista) {
                trendingList.add(fallback);
            }

            if (trendingList.size() >= safeLimit) {
                break; 
            }
        }

        return trendingList;
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