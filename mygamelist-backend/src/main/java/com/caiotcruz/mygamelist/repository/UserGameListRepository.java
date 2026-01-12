package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserGameListRepository extends JpaRepository<UserGameList, Long> {
    
    Optional<UserGameList> findByUserAndGame(User user, Game game);

    List<UserGameList> findByUser(User user);
    List<UserGameList> findByUserId(Long userId);
    Optional<UserGameList> findByUserAndIsFavoriteTrue(User user);
}