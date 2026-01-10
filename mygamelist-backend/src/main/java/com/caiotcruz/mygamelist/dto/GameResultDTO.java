package com.caiotcruz.mygamelist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Usando Record do Java 17 (muito mais limpo para DTOs de leitura)
public record GameResultDTO(
    Long id,
    String name,
    @JsonProperty("background_image") String  backgroundImage, // Mapeia o snake_case do JSON para camelCase do Java
    Double rating,
    String released
) {}