package com.caiotcruz.mygamelist.controller;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.RecommendationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
@RestController
@RequestMapping("/games/recommendations")
public class RecommendationController {
    private final RecommendationService service;
    private final UserRepository users;
    public RecommendationController(RecommendationService service,UserRepository users){this.service=service;this.users=users;}
    @GetMapping
    public RecommendationService.Response get(Principal principal){
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User user=(User)users.findByEmail(principal.getName());
        if(user==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return service.recommend(user.getId());
    }
}
