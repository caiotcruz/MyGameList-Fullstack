package com.caiotcruz.mygamelist.dto;

import com.caiotcruz.mygamelist.model.enums.GameStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddGameDTO(
    @NotNull Long rawgId, 
    GameStatus status,    
    @Min(0) @Max(10) Integer score, 
    @jakarta.validation.constraints.Size(max = 10000) String review,
    Boolean isFavorite,
    Boolean isSpoiler
) {}