package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserGameListRepository extends JpaRepository<UserGameList, Long> {
    
    // Busca um item específico (ex: Quero saber a nota que dei pro Mario)
    Optional<UserGameList> findByUserAndGame(User user, Game game);

    // Busca toda a lista de um usuário
    List<UserGameList> findByUser(User user);
    List<UserGameList> findByUserId(Long userId);
}