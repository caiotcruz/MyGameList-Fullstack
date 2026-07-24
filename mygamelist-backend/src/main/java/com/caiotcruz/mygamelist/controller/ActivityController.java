package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.dto.CreateCommentDTO;
import com.caiotcruz.mygamelist.dto.GroupedActivityDTO;
import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.service.ActivityGroupingService;
import com.caiotcruz.mygamelist.service.ActivityService;
import com.caiotcruz.mygamelist.service.SocialService;

import org.springframework.http.ResponseEntity;
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
    private final ActivityService activityService;
    private final SocialService socialService;
    private final ActivityGroupingService groupingService;

    public ActivityController(ActivityRepository activityRepository, ActivityService activityService, UserRepository userRepository,
                               SocialService socialService, ActivityGroupingService groupingService) {
        this.activityRepository = activityRepository;
        this.activityService = activityService;
        this.socialService = socialService;
        this.groupingService = groupingService;
    }

    @GetMapping
    public List<GroupedActivityDTO> getMyFeed() {
        User currentUser = activityService.getAuthenticatedUser();
        List<Activity> feed = activityRepository.findFeedByFollower(currentUser);
        return groupingService.groupAndMap(feed, currentUser.getId());
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(socialService.toggleLike(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long id, @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.ok(socialService.addComment(id, dto));
    }

}