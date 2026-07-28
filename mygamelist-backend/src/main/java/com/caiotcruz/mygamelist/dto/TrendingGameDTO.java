package com.caiotcruz.mygamelist.dto;

public record TrendingGameDTO(
        Long id,
        Long rawgId,
        String title,
        String coverUrl,
        long interactionsThisMonth
) {}