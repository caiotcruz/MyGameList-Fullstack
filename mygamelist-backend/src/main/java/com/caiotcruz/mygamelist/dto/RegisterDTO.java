package com.caiotcruz.mygamelist.dto;
import jakarta.validation.constraints.*;
import com.caiotcruz.mygamelist.infra.security.*;
public record RegisterDTO(@NotBlank @Size(min=2,max=TextLimits.NAME) String name, @NotBlank @Email @Size(max=254) String email, @PasswordInput String password) {
    public RegisterDTO {email=RequestLimits.email(email);name=name==null?null:name.trim();}
}
