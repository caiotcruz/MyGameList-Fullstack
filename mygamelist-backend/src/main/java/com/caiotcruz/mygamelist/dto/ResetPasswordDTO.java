package com.caiotcruz.mygamelist.dto;
import jakarta.validation.constraints.*;
import com.caiotcruz.mygamelist.infra.security.*;
public record ResetPasswordDTO(@NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String codigo, @PasswordInput String newPassword) {
    public ResetPasswordDTO {email=RequestLimits.email(email);}
}
