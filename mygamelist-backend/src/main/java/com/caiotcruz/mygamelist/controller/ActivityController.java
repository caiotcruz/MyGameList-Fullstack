package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.dto.CreateCommentDTO;
import com.caiotcruz.mygamelist.dto.FeedPageDTO;
import com.caiotcruz.mygamelist.service.FeedService;
import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.ActivityGroupingService;
import com.caiotcruz.mygamelist.service.ActivityService;
import com.caiotcruz.mygamelist.service.SocialService;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private final ActivityService activityService;
    private final SocialService socialService;
    private final FeedService feedService;

    public ActivityController(ActivityService activityService, SocialService socialService, FeedService feedService) {
        this.activityService = activityService;
        this.socialService = socialService;
        this.feedService = feedService;
    }

    @GetMapping
    public FeedPageDTO getMyFeed(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long before) {
        return feedService.page(activityService.getAuthenticatedUser(), before);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(socialService.toggleLike(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long id, @Valid @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.ok(socialService.addComment(id, dto));
    }

    @DeleteMapping("/{activityId}/comments/{commentId}")
    public ResponseEntity<Void> removeComment(@PathVariable Long activityId, @PathVariable Long commentId) {
        socialService.removeComment(activityId, commentId);
        return ResponseEntity.noContent().build();
    }

}