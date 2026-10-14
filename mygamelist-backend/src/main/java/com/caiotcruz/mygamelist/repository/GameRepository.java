package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    
    Optional<Game> findByRawgId(Long rawgId);

    @org.springframework.data.jpa.repository.Query("select g from Game g where g.genreSlugs is null and exists (select u.id from UserGameList u where u.game.id=g.id) and (g.genreRetryAt is null or g.genreRetryAt <= :now) order by g.id")
    java.util.List<Game> pendingGenres(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now, org.springframework.data.domain.Pageable page);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Game g set g.genreSlugs=:genres,g.genreRetryAt=:retry where g.id=:id and g.genreSlugs is null")
    void updateGenres(@org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("genres") String genres,
        @org.springframework.data.repository.query.Param("retry") java.time.LocalDateTime retry);
}