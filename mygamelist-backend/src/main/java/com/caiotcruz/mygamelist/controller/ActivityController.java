package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    @Autowired
    private ActivityRepository activityRepository;

    @GetMapping
    public List<Activity> getGlobalFeed() {

        return activityRepository.findAllByOrderByTimestampDesc(); 
    }
}