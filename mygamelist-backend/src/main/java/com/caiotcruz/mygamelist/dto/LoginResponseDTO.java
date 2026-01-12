package com.caiotcruz.mygamelist.dto;

public record LoginResponseDTO(
    String token, 
    Long userId, 
    String name
) {}