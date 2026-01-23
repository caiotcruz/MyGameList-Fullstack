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

    @Autowired 
    private ReviewVoteRepository reviewVoteRepository;

    @Autowired
    private UserRepository userRepository;

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

        long totalPlayers = userGameListRepository.countPlayersByGameId(internalId);
        long playingCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.PLAYING);
        long completedCount = userGameListRepository.countByGameIdAndStatus(internalId, GameStatus.COMPLETED);
        Double avgScore = userGameListRepository.getAverageScoreByGameId(internalId);

        String myStatus = null;
        Integer myScore = 0;
        boolean myFavorite = false;

        if (currentUserId != null) {
            Optional<UserGameList> myEntry = userGameListRepository.findByUserIdAndGameId(currentUserId, internalId);
            if (myEntry.isPresent()) {
                myStatus = myEntry.get().getStatus().name();
                myScore = myEntry.get().getScore();
                myFavorite = myEntry.get().isFavorite();
            }
        }

        List<UserGameList> reviewEntities = userGameListRepository.findReviewsByGameId(internalId);

        List<GameReviewDTO> reviews = reviewEntities.stream().map(r -> {
            long likes = reviewVoteRepository.countByReviewAndType(r, VoteType.LIKE);
            long dislikes = reviewVoteRepository.countByReviewAndType(r, VoteType.DISLIKE);
            
            int karmaScore = (int) ((likes * 2) - dislikes);
            
            String myVote = null;
            if (currentUserId != null) {
                var userObj = new User(); userObj.setId(currentUserId);
                var vote = reviewVoteRepository.findByUserAndReview(userObj, r);
                if (vote.isPresent()) myVote = vote.get().getType().name();
            }

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
        }).sorted((r1, r2) -> Integer.compare(r2.voteScore(), r1.voteScore()))
        .limit(10)
        .toList();

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