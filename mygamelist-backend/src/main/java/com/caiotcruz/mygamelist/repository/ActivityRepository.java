package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.dto.TrendingGameDTO;
import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.enums.ActivityType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
     @Query("""
        SELECT a
        FROM Activity a
        WHERE a.user = :user
           OR a.user IN (
                SELECT f.followed
                FROM UserFollow f
                WHERE f.follower = :user
           )
        ORDER BY a.timestamp DESC
    """)
    List<Activity> findFeedByFollower(@Param("user") User user);

    List<Activity> findAllByOrderByTimestampDesc();

    @Query("""
      SELECT new com.caiotcruz.mygamelist.dto.TrendingGameDTO(
         a.game.id,
         a.game.rawgId,
         a.game.title,
         a.game.coverUrl,
         COUNT(CASE WHEN a.type = 'ADDED_TO_LIST' THEN 1 END)
         + (COUNT(CASE WHEN a.type = 'REVIEWED' THEN 1 END) * 2)
      )
      FROM Activity a
      WHERE a.type IN :types
         AND a.timestamp >= :startOfMonth
      GROUP BY a.game.id, a.game.rawgId, a.game.title, a.game.coverUrl
      ORDER BY COUNT(a) DESC
   """)
   List<TrendingGameDTO> findTrendingGames(
         @Param("types") List<ActivityType> types,
         @Param("startOfMonth") LocalDateTime startOfMonth,
         Pageable pageable
   );
}