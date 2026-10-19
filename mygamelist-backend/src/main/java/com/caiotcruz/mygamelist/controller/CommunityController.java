package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.ActivityUserDTO;
import com.caiotcruz.mygamelist.dto.ApiResponseDTO;
import com.caiotcruz.mygamelist.dto.MutualFriendDTO;
import com.caiotcruz.mygamelist.dto.UserStatsDTO;
import com.caiotcruz.mygamelist.dto.UserSuggestionDTO;
import com.caiotcruz.mygamelist.dto.UserSummaryDTO;
import com.caiotcruz.mygamelist.dto.UserGameDTO;
import com.caiotcruz.mygamelist.dto.AchievementDTO;
import com.caiotcruz.mygamelist.service.AchievementService;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.repository.UserFollowRepository; 
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/community")
public class CommunityController {
    private final com.caiotcruz.mygamelist.service.CommunitySafety safety;


    private final UserRepository userRepository;
    private final UserGameListRepository listRepository;
    private final UserFollowRepository followRepository;
    private final AchievementService achievementService;

    public CommunityController(UserRepository userRepository, 
                               UserGameListRepository listRepository, 
                               UserFollowRepository followRepository, AchievementService achievementService, com.caiotcruz.mygamelist.service.CommunitySafety safety) {
        this.safety = safety;
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.followRepository = followRepository;
        this.achievementService = achievementService;
    }

    @GetMapping("/users")
    public List<UserSummaryDTO> getAllUsers() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<User> users = userRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
        
        return users.stream()
            .filter(u -> !u.getId().equals(currentUser.getId()) && !safety.blocked(currentUser.getId(),u.getId()))
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
    public List<UserGameDTO> getUserList(@PathVariable Long userId) {
        safety.requireProfile(userId);
        return listRepository.findByUserId(userId).stream().map(UserGameDTO::from).toList(); 
    }

    public record UnlockedAchievementDTO(String title, String icon, String tier, String description) {}

    @GetMapping("/users/{userId}/achievements")
    public List<UnlockedAchievementDTO> getAchievements(@PathVariable Long userId) {
        safety.requireProfile(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return achievementService.forUser(user).stream().filter(AchievementDTO::unlocked)
                .map(a -> new UnlockedAchievementDTO(a.title(), a.icon(), a.tier(), a.description())).toList();
    }

    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable Long userId) {
        safety.requireProfile(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Usuário não encontrado"));

        var hidden=safety.hiddenIds(safety.me().getId());
        List<ActivityUserDTO> followers = followRepository.findFollowersOf(user).stream().filter(u -> !hidden.contains(u.id())).toList();
        List<ActivityUserDTO> following = followRepository.findFollowingOf(user).stream().filter(u -> !hidden.contains(u.id())).toList();

        UserStatsDTO stats = new UserStatsDTO(
                followers.size(),
                following.size(),
                followers,
                following
        );

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/search")
    public List<UserSummaryDTO> searchUsers(@RequestParam String name) {
        com.caiotcruz.mygamelist.infra.security.RequestLimits.boundedQuery(name,80);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = (User) userRepository.findByEmail(email);

        List<User> users = userRepository.findByNameContainingIgnoreCase(name, org.springframework.data.domain.PageRequest.of(0, 100));

        return users.stream()
            .filter(u -> !u.getId().equals(currentUser.getId()) && !safety.blocked(currentUser.getId(),u.getId()))
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

        List<UserSuggestionDTO> suggestions = results.stream().filter(r -> !safety.blocked(currentUser.getId(), ((User)r[0]).getId())).map(result -> {
            User suggestedUser = (User) result[0]; 
            Long count = (Long) result[1];      

           List<MutualFriendDTO> mutuals = followRepository.findMutualFollowers(currentUser, suggestedUser).stream().filter(m -> !safety.hiddenIds(currentUser.getId()).contains(m.id())).toList();

            return new UserSuggestionDTO(
                suggestedUser.getId(),
                suggestedUser.getName(),
                suggestedUser.getProfilePicture(),
                mutuals,
                (long) mutuals.size()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseDTO<>("Sugestões carregadas", suggestions));
    }
}