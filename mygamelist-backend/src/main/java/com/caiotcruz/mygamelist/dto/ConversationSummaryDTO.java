package com.caiotcruz.mygamelist.dto;

import java.time.LocalDateTime;

public record ConversationSummaryDTO(
        Long conversationId,
        ActivityUserDTO otherUser,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        boolean lastMessageFromMe,
        long unreadCount
) {}