package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.AddGameDTO;
import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.enums.ActivityType;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
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
    @Autowired
    private ActivityRepository activityRepository;

    public UserGameList addGameToList(AddGameDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);

        Game game = gameService.getGameContent(dto.rawgId());

        UserGameList item = listRepository.findByUserAndGame(user, game)
                .orElse(new UserGameList());

        boolean isNew = item.getId() == null;
        GameStatus oldStatus = item.getStatus();
        Integer oldScore = item.getScore();
        String oldReview = item.getReview();

        if (isNew) {
            item.setUser(user);
            item.setGame(game);
        }

        if (dto.status() != null) item.setStatus(dto.status());
        if (dto.score() != null) item.setScore(dto.score());
        if (dto.review() != null) item.setReview(dto.review());

        item.setUpdatedAt(LocalDateTime.now());
        UserGameList savedItem = listRepository.save(item);

        if (isNew) {
            saveActivity(user, game, ActivityType.ADDED_TO_LIST, null);
        }

        if (!isNew && dto.status() != null && oldStatus != dto.status()) {
            saveActivity(user, game, ActivityType.CHANGED_STATUS, dto.status().toString());
        }

        if (dto.score() != null && !dto.score().equals(oldScore)) {
            saveActivity(user, game, ActivityType.RATED, String.valueOf(dto.score()));
        }
        
        if (dto.review() != null && !dto.review().isEmpty() && !dto.review().equals(oldReview)) {
            saveActivity(user, game, ActivityType.REVIEWED, null);
        }

        return savedItem;
    }

    private void saveActivity(User user, Game game, ActivityType type, String detail) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setGame(game);
        activity.setType(type);
        activity.setDetail(detail);
        activityRepository.save(activity);
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