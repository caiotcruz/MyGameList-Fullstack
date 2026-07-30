package com.caiotcruz.mygamelist.dto;

public record RelatedGameDTO(
        Long rawgId,
        String name,
        String backgroundImage,
        String released,
        Integer metacritic
) {}