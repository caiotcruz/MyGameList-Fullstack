package com.caiotcruz.mygamelist.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
    @NotBlank @Email String email,
    @NotBlank String codigo,
    @NotBlank @Size(min = 6) String newPassword
) {}