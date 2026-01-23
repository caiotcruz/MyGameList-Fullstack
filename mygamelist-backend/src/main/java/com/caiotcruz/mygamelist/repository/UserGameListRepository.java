package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.dto.GameReviewDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.enums.GameStatus; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGameListRepository extends JpaRepository<UserGameList, Long> {
    
    Optional<UserGameList> findByUserAndGame(User user, Game game);
    List<UserGameList> findByUser(User user);
    List<UserGameList> findByUserId(Long userId);
    Optional<UserGameList> findByUserAndIsFavoriteTrue(User user);


    @Query("SELECT COUNT(ugl) FROM UserGameList ugl WHERE ugl.game.id = :gameId")
    long countPlayersByGameId(@Param("gameId") Long gameId);

    @Query("SELECT COUNT(ugl) FROM UserGameList ugl WHERE ugl.game.id = :gameId AND ugl.status = :status")
    long countByGameIdAndStatus(@Param("gameId") Long gameId, @Param("status") GameStatus status);

    @Query("SELECT AVG(ugl.score) FROM UserGameList ugl WHERE ugl.game.id = :gameId AND ugl.score > 0")
    Double getAverageScoreByGameId(@Param("gameId") Long gameId);

    @Query("SELECT ugl FROM UserGameList ugl WHERE ugl.user.id = :userId AND ugl.game.id = :gameId")
    Optional<UserGameList> findByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);
    
   @Query("SELECT count(ugl) FROM UserGameList ugl WHERE ugl.game.rawgId = :rawgId")
    long countPlayersByRawgId(@Param("rawgId") Long rawgId);

    @Query("""
        SELECT new com.caiotcruz.mygamelist.dto.GameReviewDTO(
            u.name, u.profilePicture, ugl.score, ugl.review, ugl.updatedAt
        )
        FROM UserGameList ugl
        JOIN ugl.user u
        WHERE ugl.game.id = :gameId 
        AND ugl.review IS NOT NULL 
        AND LENGTH(ugl.review) > 2
        ORDER BY ugl.updatedAt DESC
        LIMIT 5
    """)
    List<GameReviewDTO> findLatestReviews(@Param("gameId") Long gameId);
}