package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.GameHubDTO;
import com.caiotcruz.mygamelist.dto.GameResultDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

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
}