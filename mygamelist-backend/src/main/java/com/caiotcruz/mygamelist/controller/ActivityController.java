package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.Comment;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.SocialService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/community/activities")
public class ActivityController {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final SocialService socialService;

    public ActivityController(
            ActivityRepository activityRepository,
            UserRepository userRepository,
            SocialService socialService) {

        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.socialService = socialService;
    }

    @GetMapping
    public List<Activity> getMyFeed() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<Activity> feed = activityRepository.findFeedByFollower(currentUser);
        
        return feed;
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long id) {
        boolean isLiked = socialService.toggleLike(id);
        return ResponseEntity.ok(isLiked); 
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long id, @RequestBody CommentDTO dto) {
        Comment newComment = socialService.addComment(id, dto);
        return ResponseEntity.ok(newComment);
    }
    
}