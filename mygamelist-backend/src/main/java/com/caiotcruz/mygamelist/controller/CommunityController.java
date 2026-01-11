package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.UserSummaryDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.UserFollowRepository; // 👈 Novo Import
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder; // 👈 Novo Import
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGameListRepository listRepository;

    @Autowired
    private UserFollowRepository followRepository; 

    @GetMapping("/users")
    public List<UserSummaryDTO> getAllUsers() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<User> users = userRepository.findAll();
        
        return users.stream()
                .filter(u -> !u.getId().equals(currentUser.getId())) 
                .map(u -> {
                    boolean isFollowing = followRepository.findByFollowerAndFollowed(currentUser, u).isPresent();
                    
                    return new UserSummaryDTO(u.getId(), u.getName(), isFollowing);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/users/{userId}/list")
    public List<UserGameList> getUserList(@PathVariable Long userId) {

        return listRepository.findByUserId(userId); 
    }
}