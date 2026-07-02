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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserGameListService {

    private final UserGameListRepository listRepository;
    private final GameService gameService;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    public UserGameListService(UserGameListRepository listRepository, GameService gameService, UserRepository userRepository, ActivityRepository activityRepository) {
        this.listRepository = listRepository;
        this.gameService = gameService;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }
    
    public UserGameList addGameToList(AddGameDTO dto) {
        User user = getAuthenticatedUser();
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

        handleFavoriteConstraint(user, item, dto.isFavorite());
        updateFields(item, dto);

        item.setUpdatedAt(LocalDateTime.now());
        UserGameList savedItem = listRepository.save(item);

        createActivities(user, game, isNew, dto, oldStatus, oldScore, oldReview);

        return savedItem;
    }

    public List<UserGameList> getMyList() {
        User user = getAuthenticatedUser();
        return listRepository.findByUser(user);
    }

    public void removeItem(Long listId) {
        User user = getAuthenticatedUser();

        UserGameList item = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para deletar este item.");
        }

        listRepository.delete(item);
    }

    // --- Métodos Privados de Refatoração ---

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    private void updateFields(UserGameList item, AddGameDTO dto) {
        if (dto.isFavorite() != null) item.setFavorite(dto.isFavorite());
        if (dto.status() != null) item.setStatus(dto.status());
        if (dto.score() != null) item.setScore(dto.score());
        if (dto.review() != null) item.setReview(dto.review());
    }

    private void handleFavoriteConstraint(User user, UserGameList item, Boolean isFavorite) {
        if (Boolean.TRUE.equals(isFavorite)) {
            listRepository.findByUserAndIsFavoriteTrue(user).ifPresent(existingFav -> {
                if (!existingFav.getId().equals(item.getId())) {
                    existingFav.setFavorite(false);
                    listRepository.save(existingFav);
                }
            });
        }
    }

    private void createActivities(User user, Game game, boolean isNew, AddGameDTO dto, 
                                  GameStatus oldStatus, Integer oldScore, String oldReview) {
        if (isNew) {
            createActivity(user, game, ActivityType.ADDED_TO_LIST, null);
        } 

        boolean statusMudou = dto.status() != null && oldStatus != dto.status();
        if (statusMudou) {
            createActivity(user, game, ActivityType.CHANGED_STATUS, dto.status().toString());
        }

        if (dto.score() != null && dto.score() > 0) {
            boolean notaMudou = !dto.score().equals(oldScore);
            if (notaMudou) {
                createActivity(user, game, ActivityType.RATED, String.valueOf(dto.score()));
            }
        }

        if (dto.review() != null && !dto.review().isEmpty() && !dto.review().equals(oldReview)) {
            createActivity(user, game, ActivityType.REVIEWED, dto.review());
        }
    }

    private void createActivity(User user, Game game, ActivityType type, String detail) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setGame(game);
        activity.setType(type);
        activity.setDetail(detail);
        activityRepository.save(activity);
    }
}