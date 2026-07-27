package com.caiotcruz.mygamelist.dto;
import java.time.LocalDateTime;

public record GameReviewDTO(
    Long reviewId,
    String userName,
    Long userId,
    String userAvatar,
    Integer score,
    String review,
    boolean isSpoiler,
    LocalDateTime date,
    long likesCount,
    long dislikesCount,
    int voteScore,
    String myVote
) {}