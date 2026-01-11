package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserFollowController {

    @Autowired
    private UserFollowService followService;

    @PostMapping("/{id}/follow")
    public ResponseEntity follow(@PathVariable Long id) {
        followService.followUser(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/unfollow")
    public ResponseEntity unfollow(@PathVariable Long id) {
        followService.unfollowUser(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/is-following")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long id) {
        return ResponseEntity.ok(followService.isFollowing(id));
    }
}