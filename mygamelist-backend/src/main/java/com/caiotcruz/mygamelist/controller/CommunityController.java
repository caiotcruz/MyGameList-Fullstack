package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.ApiResponseDTO;
import com.caiotcruz.mygamelist.dto.MutualFriendDTO;
import com.caiotcruz.mygamelist.dto.UserSuggestionDTO;
import com.caiotcruz.mygamelist.dto.UserSummaryDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.UserFollowRepository; 
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/community")
public class CommunityController {

    private final UserRepository userRepository;
    private final UserGameListRepository listRepository;
    private final UserFollowRepository followRepository;

    public CommunityController(UserRepository userRepository, 
                               UserGameListRepository listRepository, 
                               UserFollowRepository followRepository) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.followRepository = followRepository;
    }

    @GetMapping("/users")
    public List<UserSummaryDTO> getAllUsers() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<User> users = userRepository.findAll();
        
        return users.stream()
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .map(u -> {
                boolean isFollowing = followRepository.findByFollowerAndFollowed(currentUser, u).isPresent();
                
                return new UserSummaryDTO(
                    u.getId(), 
                    u.getName(), 
                    isFollowing, 
                    u.getProfilePicture(),
                    u.isRotatingAvatar()
                );
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/users/{userId}/list")
    public List<UserGameList> getUserList(@PathVariable Long userId) {

        return listRepository.findByUserId(userId); 
    }

    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getUserStats(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        long following = followRepository.countByFollower(user);
        long followers = followRepository.countByFollowed(user);

        Map<String, Long> stats = new HashMap<>();
        stats.put("following", following);
        stats.put("followers", followers);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/search")
    public List<UserSummaryDTO> searchUsers(@RequestParam String name) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<User> users = userRepository.findByNameContainingIgnoreCase(name);

        return users.stream()
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .map(u -> {
                boolean isFollowing = followRepository.findByFollowerAndFollowed(currentUser, u).isPresent();
                
                return new UserSummaryDTO(
                    u.getId(), 
                    u.getName(), 
                    isFollowing, 
                    u.getProfilePicture(),
                    u.isRotatingAvatar()
                );
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponseDTO<List<UserSuggestionDTO>>> getSuggestions() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<Object[]> results = userRepository.findSuggestionsWithMutualCount(currentUser);

        List<UserSuggestionDTO> suggestions = results.stream().map(result -> {
            User suggestedUser = (User) result[0]; 
            Long count = (Long) result[1];      

           List<MutualFriendDTO> mutuals = followRepository.findMutualFollowers(currentUser, suggestedUser);

            return new UserSuggestionDTO(
                suggestedUser.getId(),
                suggestedUser.getName(),
                suggestedUser.getProfilePicture(),
                mutuals,
                count
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseDTO<>("Sugestões carregadas", suggestions));
    }
}