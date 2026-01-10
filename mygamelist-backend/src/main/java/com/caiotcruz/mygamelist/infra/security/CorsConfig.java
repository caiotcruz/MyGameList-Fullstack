package com.caiotcruz.mygamelist.infra.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // AQUI É O PULO DO GATO: Adicione a URL do Vercel e o Localhost
                .allowedOrigins(
                    "http://localhost:4200", 
                    "https://my-game-list-fullstack.vercel.app" 
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT");
    }
}