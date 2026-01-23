package com.caiotcruz.mygamelist.dto;
import java.time.LocalDateTime;

public record GameReviewDTO(
    Long reviewId,
    String userName,
    String userAvatar,
    Integer score,
    String review,
    LocalDateTime date,
    long likesCount,
    long dislikesCount,
    int voteScore,
    String myVote
) {}