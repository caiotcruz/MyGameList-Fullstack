package com.caiotcruz.mygamelist.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.caiotcruz.mygamelist.dto.ActivityDTO;
import com.caiotcruz.mygamelist.dto.ActivityGameDTO;
import com.caiotcruz.mygamelist.dto.ActivityUserDTO;
import com.caiotcruz.mygamelist.dto.CommentDTO;
import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.UserRepository;

@Service
public class ActivityService {
    
    private final UserRepository userRepository;

    public ActivityService(
        UserRepository userRepository
    ){
        this.userRepository = userRepository;
    }

    public User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    public class ActivityMapper {

        public static ActivityDTO toDTO(Activity activity, Long currentUserId) {
            boolean likedByMe = activity.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUserId));

            List<CommentDTO> comments = activity.getComments().stream()
                    .map(CommentDTO::from)
                    .toList();

            return new ActivityDTO(
                    activity.getId(),
                    activity.getType(),
                    activity.getDetail(),
                    activity.getTimestamp(),
                    ActivityUserDTO.from(activity.getUser()),
                    ActivityGameDTO.from(activity.getGame()),
                    activity.getLikes().size(),
                    likedByMe,
                    comments
            );
        }
    }
}
