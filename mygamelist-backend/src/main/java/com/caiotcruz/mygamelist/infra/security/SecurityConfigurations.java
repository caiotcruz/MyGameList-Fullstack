package com.caiotcruz.mygamelist.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @org.springframework.beans.factory.annotation.Value("${app.cors-origins:http://localhost:4200}")
    private String allowedOrigins;
    private final SecurityFilter securityFilter;
    private final AuthRateLimitFilter authRateLimitFilter;

    public SecurityConfigurations(SecurityFilter securityFilter, AuthRateLimitFilter authRateLimitFilter) {
        this.securityFilter = securityFilter;
        this.authRateLimitFilter = authRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((req, res, ex) -> {
                        res.setStatus(401);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"message\":\"Sessão inválida ou expirada. Entre novamente.\"}");
                    })
                    .accessDeniedHandler((req, res, ex) -> {
                        res.setStatus(403);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"message\":\"Você não tem permissão para esta ação.\"}");
                    }))
                .authorizeHttpRequests(req -> req
                     .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register", "/auth/verify",
                            "/auth/forgot-password", "/auth/reset-password", "/auth/resend-verification").permitAll()
                    .anyRequest().authenticated())
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}