package com.caiotcruz.mygamelist.dto;
import jakarta.validation.constraints.*;
import com.caiotcruz.mygamelist.infra.security.*;
public record LoginDTO(@NotBlank @Email @Size(max=254) String email, @PasswordInput(creation=false,message="Senha inválida.") String password) {
    public LoginDTO {email=RequestLimits.email(email);}
}
