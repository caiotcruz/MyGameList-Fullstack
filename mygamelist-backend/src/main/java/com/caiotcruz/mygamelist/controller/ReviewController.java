package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final GameService gameService;
    private final UserRepository userRepository;

    public ReviewController(GameService gameService, UserRepository userRepository) {
        this.gameService = gameService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<Void> vote(@PathVariable Long reviewId, @RequestBody Map<String, String> body) {
        String type = body.get("type");
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);

        gameService.voteOnReview(reviewId, user.getId(), type);
        
        return ResponseEntity.ok().build();
    }
}