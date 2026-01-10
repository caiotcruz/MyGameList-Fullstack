package com.caiotcruz.mygamelist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GameResultDTO(
    Long id,
    String name,
    @JsonProperty("background_image") String  backgroundImage, 
    Double rating,
    String released
) {}