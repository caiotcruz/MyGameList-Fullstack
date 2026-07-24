package com.caiotcruz.mygamelist.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.caiotcruz.mygamelist.model.enums.ActivityType;

public record ActivityDTO(
        Long id,
        ActivityType type,
        String detail,
        LocalDateTime timestamp,
        ActivityUserDTO user,
        ActivityGameDTO game,
        int likesCount,
        boolean likedByMe,
        List<CommentDTO> comments
) {}