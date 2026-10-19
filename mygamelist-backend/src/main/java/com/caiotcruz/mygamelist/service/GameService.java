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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GameService {
    private final UserLocks locks;

    private final CommunitySafety safety;


    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final UserGameListRepository userGameListRepository;
    private final RawgClient rawgClient;
    private final ReviewVoteRepository reviewVoteRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @Value("${api.rawg.key}")
    private String apiKey;

    public GameService(GameRepository gameRepository, UserGameListRepository userGameListRepository, RawgClient rawgClient, ReviewVoteRepository reviewVoteRepository, UserRepository userRepository, ActivityRepository activityRepository, CommunitySafety safety, UserLocks locks) {
        this.locks = locks;
        this.safety = safety;
        this.gameRepository = gameRepository;
        this.userGameListRepository = userGameListRepository;
        this.rawgClient = rawgClient;
        this.reviewVoteRepository = reviewVoteRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }

    public List<GameResultDTO> searchGames(String query, Integer page) {
        com.caiotcruz.mygamelist.infra.security.RequestLimits.boundedQuery(query,200);
        int pageNumber=page==null?1:page;
        if(pageNumber<1 || pageNumber>100)throw new IllegalArgumentException("Invalid page");
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
                    if(externalGame.genres()!=null) newGame.setGenreSlugs(GenreMetadata.slugs(externalGame.genres()));

                    return gameRepository.save(newGame);
                });
    }

    @Transactional
    public void voteOnReview(Long reviewId, Long userId, String voteTypeStr) {
        var review = userGameListRepository.findById(reviewId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Review não encontrada"));

        locks.lock(userId, review.getUser().getId());
        safety.requireVisible(userId, review.getUser().getId());
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Usuário não encontrado"));

        if (review.getReview() == null || review.getReview().isBlank()) {
            throw new IllegalArgumentException("Review inexistente");
        }
        if (review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Você não pode votar na própria review");
        }
        if(voteTypeStr==null) throw new IllegalArgumentException("Informe o tipo de voto.");
        VoteType type = VoteType.valueOf(voteTypeStr.toUpperCase(java.util.Locale.ROOT));

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
                stats.droppedCount(),
                stats.avgScore(),
                stats.scoreDistribution(),
                myEntry.status(),
                myEntry.score(),
                myEntry.favorite(),
                myEntry.review(),
                myEntry.spoiler(),
                reviews
        );
    }

    public List<RelatedGameDTO> getAdditions(Long rawgId) {
        try {
            return rawgClient.getAdditions(apiKey, rawgId).results().stream()
                    .map(g -> new RelatedGameDTO(g.id(), g.name(), g.backgroundImage(), g.released(), g.metacritic()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar additions da RAWG para o jogo {}: {}", rawgId, e.getClass().getSimpleName());
            return List.of();
        }
    }

    public List<RelatedGameDTO> getGameSeries(Long rawgId) {
        try {
            return rawgClient.getGameSeries(apiKey, rawgId).results().stream()
                    .map(g -> new RelatedGameDTO(g.id(), g.name(), g.backgroundImage(), g.released(), g.metacritic()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar game-series da RAWG para o jogo {}: {}", rawgId, e.getClass().getSimpleName());
            return List.of();
        }
    }

    public List<GameScreenshotDTO> getScreenshots(Long rawgId) {
        try {
            return rawgClient.getScreenshots(apiKey, rawgId).results().stream()
                    .map(s -> new GameScreenshotDTO(s.id(), s.image()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar screenshots da RAWG para o jogo {}: {}", rawgId, e.getClass().getSimpleName());
            return List.of();
        }
    }

    public List<GameStoreDTO> getStores(Long rawgId) {
        try {
            return rawgClient.getStores(apiKey, rawgId).results().stream()
                    .map(s -> new GameStoreDTO(s.storeId(), RawgStoreCatalog.nameFor(s.storeId()), s.url()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar stores da RAWG para o jogo {}: {}", rawgId, e.getClass().getSimpleName());
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
            log.warn("Falha ao buscar achievements da RAWG para o jogo {}: {}", rawgId, e.getClass().getSimpleName());
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
            log.warn("Falha ao buscar trailers da RAWG para o jogo {}: {}", rawgId, e.getClass().getSimpleName());
            return List.of();
        }
    }

    private GameStats loadStatistics(Long internalId) {
        long totalPlayers = userGameListRepository.countPlayersByGameId(internalId);
        long playingCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.PLAYING);
        long completedCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.COMPLETED);
        long platinumCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.PLATINUM);
        long droppedCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.DROPPED);
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

        return new GameStats(totalPlayers, playingCount, completedCount, platinumCount, droppedCount, avgScore != null ? avgScore : 0.0, scoreMap);
    }

    private UserGameEntry loadMyEntry(Long internalId, Long currentUserId) {
        if (currentUserId == null) {
            return new UserGameEntry(null, 0, false, null, false);
        }

        return userGameListRepository.findByUserIdAndGameId(currentUserId, internalId)
                .map(entry -> new UserGameEntry(entry.getStatus() == null ? null : entry.getStatus().name(), entry.getScore(), entry.isFavorite(), entry.getReview(), entry.isSpoiler()))
                .orElseGet(() -> new UserGameEntry(null, 0, false, null, false));
    }

    private List<GameReviewDTO> loadReviews(Long internalId, Long currentUserId) {
        List<UserGameList> reviewEntities = userGameListRepository.findReviewsByGameId(internalId,PageRequest.of(0,100));

        var hidden=safety.hiddenIds(currentUserId);
        return reviewEntities.stream().filter(r -> !hidden.contains(r.getUser().getId())).map(r -> {
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

        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        List<ActivityType> relevantTypes = List.of(ActivityType.ADDED_TO_LIST, ActivityType.REVIEWED);

        List<TrendingGameDTO> trendingList = new java.util.ArrayList<>(
                activityRepository.findTrendingGames(
                        relevantTypes,
                        startOfMonth,
                        PageRequest.of(0, safeLimit)
                )
        );

        return trendingList;
    }

    private record GameStats(
        long totalPlayers,
        long playingCount,
        long completedCount,
        long platinumCount,
        long droppedCount,
        double avgScore,
        Map<Integer, Long> scoreDistribution
    ) {}

    private record UserGameEntry(
        String status,
        Integer score,
        boolean favorite,
        String review,
        boolean spoiler
    ) {}
}
