package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.AddGameDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserGameListService {

    @Autowired
    private UserGameListRepository listRepository;
    @Autowired
    private GameService gameService;
    @Autowired
    private UserRepository userRepository;

    public UserGameList addGameToList(AddGameDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);

        Game game = gameService.getGameContent(dto.rawgId());

        UserGameList item = listRepository.findByUserAndGame(user, game)
                .orElse(new UserGameList());

        if (item.getId() == null) {
            item.setUser(user);
            item.setGame(game);
        }
        
        if (dto.status() != null) item.setStatus(dto.status());
        if (dto.score() != null) item.setScore(dto.score());
        if (dto.review() != null) item.setReview(dto.review());
        
        item.setUpdatedAt(LocalDateTime.now());

        return listRepository.save(item);
    }
    
    public java.util.List<UserGameList> getMyList() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);
        return listRepository.findByUser(user);
    }

    public void removeItem(Long listId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);

        UserGameList item = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para deletar este item.");
        }

        listRepository.delete(item);
    }
}