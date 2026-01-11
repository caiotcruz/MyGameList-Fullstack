package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Activity> getMyFeed() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<Activity> feed = activityRepository.findFeedByFollower(currentUser);
        
        return feed;
    }
}