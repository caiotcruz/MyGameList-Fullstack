package com.caiotcruz.mygamelist.dto;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import java.time.LocalDateTime;
public record UserGameDTO(Long id, PublicUserDTO user, ActivityGameDTO game,
        GameStatus status, Integer score, String review, boolean favorite,
        boolean spoiler, LocalDateTime updatedAt) {
    public static UserGameDTO from(UserGameList e) {
        return new UserGameDTO(e.getId(), PublicUserDTO.from(e.getUser()),
                ActivityGameDTO.from(e.getGame()), e.getStatus(), e.getScore(), e.getReview(),
                e.isFavorite(), e.isSpoiler(), e.getUpdatedAt());
    }
}
