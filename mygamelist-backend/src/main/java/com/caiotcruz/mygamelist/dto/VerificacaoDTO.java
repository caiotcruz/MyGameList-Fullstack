package com.caiotcruz.mygamelist.dto;
import jakarta.validation.constraints.*;
import com.caiotcruz.mygamelist.infra.security.*;
public record VerificacaoDTO(@NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="[0-9]{6}") String codigo) {
    public VerificacaoDTO {email=RequestLimits.email(email);}
}
