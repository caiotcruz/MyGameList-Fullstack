package com.caiotcruz.mygamelist.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificacaoDTO(
    @NotBlank @Email String email,
    @NotBlank String codigo
) {}