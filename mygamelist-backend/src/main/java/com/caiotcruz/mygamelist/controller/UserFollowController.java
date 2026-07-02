package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.service.UserFollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserFollowController {

    private final UserFollowService followService;

    public UserFollowController(UserFollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long id) {
        followService.followUser(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/unfollow")
    public ResponseEntity<Void> unfollow(@PathVariable Long id) {
        followService.unfollowUser(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/is-following")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long id) {
        return ResponseEntity.ok(followService.isFollowing(id));
    }
}