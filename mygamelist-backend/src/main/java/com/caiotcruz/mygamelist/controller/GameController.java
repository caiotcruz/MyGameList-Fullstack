package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.GameAchievementDTO;
import com.caiotcruz.mygamelist.dto.GameHubDTO;
import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.dto.GameScreenshotDTO;
import com.caiotcruz.mygamelist.dto.GameStoreDTO;
import com.caiotcruz.mygamelist.dto.GameTrailerDTO;
import com.caiotcruz.mygamelist.dto.RelatedGameDTO;
import com.caiotcruz.mygamelist.dto.TrendingGameDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;
    private final UserRepository userRepository;

    public GameController(GameService gameService, UserRepository userRepository) {
        this.gameService = gameService;
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public List<GameResultDTO> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") Integer page
    ) {
        return gameService.searchGames(query, page);
    }

    @GetMapping("/{id}")
    public Game getGameDetails(@PathVariable Long id) {
        return gameService.getGameContent(id);
    }

    @GetMapping("/{id}/hub")
    public ResponseEntity<GameHubDTO> getGameHub(@PathVariable Long id) {
        Long userId = null;

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                User user = (User) userRepository.findByEmail(email);
                if (user != null) {
                    userId = user.getId();
                }
            }
        } catch (Exception e) {
            System.out.println("Acesso anônimo ao Hub ou erro de auth: " + e.getMessage());
        }

        GameHubDTO hubData = gameService.getGameHubData(id, userId);
        
        return ResponseEntity.ok(hubData);
    }

    @GetMapping("/trending")
    public List<TrendingGameDTO> getTrendingGames(@RequestParam(defaultValue = "10") int limit) {
        return gameService.getTrendingGames(limit);
    }

    @GetMapping("/{rawgId}/additions")
    public List<RelatedGameDTO> getAdditions(@PathVariable Long rawgId) {
        return gameService.getAdditions(rawgId);
    }

    @GetMapping("/{rawgId}/series")
    public List<RelatedGameDTO> getGameSeries(@PathVariable Long rawgId) {
        return gameService.getGameSeries(rawgId);
    }

    @GetMapping("/{rawgId}/screenshots")
    public List<GameScreenshotDTO> getScreenshots(@PathVariable Long rawgId) {
        return gameService.getScreenshots(rawgId);
    }

    @GetMapping("/{rawgId}/stores")
    public List<GameStoreDTO> getStores(@PathVariable Long rawgId) {
        return gameService.getStores(rawgId);
    }

    @GetMapping("/{rawgId}/achievements")
    public List<GameAchievementDTO> getAchievements(@PathVariable Long rawgId) {
        return gameService.getAchievements(rawgId);
    }

    @GetMapping("/{rawgId}/trailers")
    public List<GameTrailerDTO> getTrailers(@PathVariable Long rawgId) {
        return gameService.getTrailers(rawgId);
    }
}