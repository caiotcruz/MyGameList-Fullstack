package com.caiotcruz.mygamelist.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.caiotcruz.mygamelist.model.Activity;
import com.caiotcruz.mygamelist.model.enums.ActivityType;

public record GroupedActivityDTO(
        Long id,
        LocalDateTime timestamp,
        ActivityUserDTO user,
        ActivityGameDTO game,
        List<ActivityType> types,
        String statusDetail,
        Integer ratingValue,
        String reviewText,
        int likesCount,
        boolean likedByMe,
        List<CommentDTO> comments
) {
    public static GroupedActivityDTO from(List<Activity> group, Long currentUserId) {
        Activity primary = group.get(0);

        List<ActivityType> types = group.stream().map(Activity::getType).toList();

        String statusDetail = group.stream()
                .filter(a -> a.getType() == ActivityType.CHANGED_STATUS)
                .map(Activity::getDetail).findFirst().orElse(null);

        Integer ratingValue = group.stream()
                .filter(a -> a.getType() == ActivityType.RATED)
                .map(a -> Integer.valueOf(a.getDetail())).findFirst().orElse(null);

        String reviewText = group.stream()
                .filter(a -> a.getType() == ActivityType.REVIEWED)
                .map(Activity::getDetail).findFirst().orElse(null);

        boolean likedByMe = primary.getLikes().stream()
                .anyMatch(l -> l.getUser().getId().equals(currentUserId));

        List<CommentDTO> comments = primary.getComments().stream()
                .map(CommentDTO::from).toList();

        return new GroupedActivityDTO(
                primary.getId(), primary.getTimestamp(),
                ActivityUserDTO.from(primary.getUser()), ActivityGameDTO.from(primary.getGame()),
                types, statusDetail, ratingValue, reviewText,
                primary.getLikes().size(), likedByMe, comments
        );
    }
}