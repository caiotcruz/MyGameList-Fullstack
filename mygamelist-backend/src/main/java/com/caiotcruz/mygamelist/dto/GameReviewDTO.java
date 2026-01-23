package com.caiotcruz.mygamelist.dto;
import java.time.LocalDateTime;

public record GameReviewDTO(
    String userName,
    String userAvatar,
    Integer score,
    String review,
    LocalDateTime date
) {}