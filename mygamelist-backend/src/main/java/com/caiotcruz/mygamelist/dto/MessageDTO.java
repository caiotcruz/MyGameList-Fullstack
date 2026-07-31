package com.caiotcruz.mygamelist.dto;

import java.time.LocalDateTime;

public record MessageDTO(
        Long id,
        Long senderId,
        String content,
        LocalDateTime sentAt,
        boolean mine
) {}