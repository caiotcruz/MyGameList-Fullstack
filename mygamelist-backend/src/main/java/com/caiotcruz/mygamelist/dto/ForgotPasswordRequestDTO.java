package com.caiotcruz.mygamelist.dto;
import jakarta.validation.constraints.*;
import com.caiotcruz.mygamelist.infra.security.*;
public record ForgotPasswordRequestDTO(@NotBlank @Email @Size(max=254) String email) {
    public ForgotPasswordRequestDTO {email=RequestLimits.email(email);}
}
