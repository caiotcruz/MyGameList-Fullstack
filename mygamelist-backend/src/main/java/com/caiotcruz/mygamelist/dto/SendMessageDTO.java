package com.caiotcruz.mygamelist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageDTO(
        @NotBlank @Size(max = TextLimits.MESSAGE) String content
) {}
