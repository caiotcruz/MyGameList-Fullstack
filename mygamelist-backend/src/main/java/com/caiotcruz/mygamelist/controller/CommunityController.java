package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.UserSummaryDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/users")
    public List<UserSummaryDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(u -> new UserSummaryDTO(u.getId(), u.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/users/{userId}/list")
    public List<UserGameList> getUserList(@PathVariable Long userId) {
        return listRepository.findByUserId(userId);
    }
}