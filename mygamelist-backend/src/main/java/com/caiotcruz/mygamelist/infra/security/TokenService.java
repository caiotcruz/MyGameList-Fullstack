package com.caiotcruz.mygamelist.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("mygamelist") 
                    .withSubject(user.getEmail())
                    .withClaim("version", user.getTokenVersion()) 
                    .withExpiresAt(genExpirationDate()) 
                    .sign(algorithm); 
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("mygamelist")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            System.out.println("Token inválido: " + exception.getMessage());
            return "";
        }
    }

    public boolean matchesVersion(String token, User user) {
        try {
            Integer version = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("mygamelist").build().verify(token).getClaim("version").asInt();
            return version != null && version == user.getTokenVersion();
        } catch (JWTVerificationException ex) {
            return false;
        }
    }

    private Instant genExpirationDate() {
        return Instant.now().plus(2, java.time.temporal.ChronoUnit.HOURS);
    }
}