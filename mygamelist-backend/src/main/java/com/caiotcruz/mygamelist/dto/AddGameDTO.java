package com.caiotcruz.mygamelist.dto;

import com.caiotcruz.mygamelist.model.enums.GameStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddGameDTO(
    @NotNull Long rawgId, // O ID externo (ex: 58175)
    GameStatus status,    // Opcional (se nulo, assumimos PLAN_TO_PLAY)
    @Min(0) @Max(10) Integer score, // Opcional
    String review // Opcional
) {}