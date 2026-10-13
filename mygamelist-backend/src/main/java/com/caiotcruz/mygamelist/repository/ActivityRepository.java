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
    
    interface FeedGroup {
        String getGroupKey();
        Long getAnchorId();
    }

    @Query(value = """
        SELECT COALESCE(a.group_id, 'legacy-' || CAST(a.id AS text)) AS "groupKey",
               MAX(a.id) AS "anchorId"
        FROM tb_activities a
        WHERE (a.user_id = :userId OR a.user_id IN (
            SELECT f.followed_id FROM tb_follows f WHERE f.follower_id = :userId
        ))
        AND NOT EXISTS(SELECT 1 FROM tb_user_blocks b WHERE (b.blocker_id=:userId AND b.blocked_id=a.user_id) OR (b.blocked_id=:userId AND b.blocker_id=a.user_id))
        GROUP BY COALESCE(a.group_id, 'legacy-' || CAST(a.id AS text))
        HAVING MAX(a.id) < :beforeId
        ORDER BY MAX(a.id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<FeedGroup> findFeedGroups(@Param("userId") Long userId,
                                  @Param("beforeId") long beforeId, @Param("limit") int limit);

    @Query(value = """
        SELECT a.* FROM tb_activities a
        WHERE COALESCE(a.group_id, 'legacy-' || CAST(a.id AS text)) IN (:keys)
          AND (a.user_id = :userId OR a.user_id IN (
              SELECT f.followed_id FROM tb_follows f WHERE f.follower_id = :userId
          ))
        AND NOT EXISTS(SELECT 1 FROM tb_user_blocks b WHERE (b.blocker_id=:userId AND b.blocked_id=a.user_id) OR (b.blocked_id=:userId AND b.blocker_id=a.user_id))
        ORDER BY a.id DESC
        """, nativeQuery = true)
    List<Activity> findVisibleGroupEvents(@Param("userId") Long userId, @Param("keys") List<String> keys);

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
      ORDER BY COUNT(CASE WHEN a.type = 'ADDED_TO_LIST' THEN 1 END) + (COUNT(CASE WHEN a.type = 'REVIEWED' THEN 1 END) * 2) DESC
   """)
   List<TrendingGameDTO> findTrendingGames(
         @Param("types") List<ActivityType> types,
         @Param("startOfMonth") LocalDateTime startOfMonth,
         Pageable pageable
   );
}