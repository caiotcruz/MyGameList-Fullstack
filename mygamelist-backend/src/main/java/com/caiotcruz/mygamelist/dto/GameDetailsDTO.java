package com.caiotcruz.mygamelist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GameDetailsDTO(
    Long id,
    String name,
    @JsonProperty("description_raw") String description, 
    @JsonProperty("background_image") String backgroundImage,
    String released
) {}