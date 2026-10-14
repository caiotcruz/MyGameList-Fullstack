package com.caiotcruz.mygamelist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GameDetailsDTO(
    Long id,
    String name,
    @JsonProperty("description_raw") String description,
    @JsonProperty("background_image") String backgroundImage,
    String released,
    Integer metacritic,
    java.util.List<com.caiotcruz.mygamelist.dto.rawg.RawgGenre> genres
) {
    public GameDetailsDTO(Long id,String name,String description,String backgroundImage,String released,Integer metacritic) {
        this(id,name,description,backgroundImage,released,metacritic,null);
    }
}