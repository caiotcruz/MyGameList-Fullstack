package com.caiotcruz.mygamelist.dto.rawg;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RawgGameSummaryDTO(
        Long id,
        String name,
        @JsonProperty("background_image") String backgroundImage,
        String released,
        Integer metacritic
) {}