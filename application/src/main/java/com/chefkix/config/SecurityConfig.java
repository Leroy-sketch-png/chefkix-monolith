package com.chefkix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Unified security configuration for the ChefKix monolith.
 * Merges public endpoints from all former microservices.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Public endpoints that do NOT require authentication.
     * CRITICAL: Must match ACTUAL controller @XxxMapping paths exactly.
     * Audited 2025-03 against all controllers in identity/notification modules.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // --- Auth (identity) — pre-login flows only ---
            "/auth/login",                  // AuthenticationController POST /login
            "/auth/register",               // AuthenticationController POST /register
            "/auth/refresh-token",          // AuthenticationController POST /refresh-token
            "/auth/verify-otp",             // OtpController POST /verify-otp (signup OTP)
            "/auth/resend-otp",             // OtpController POST /resend-otp
            "/auth/forgot-password",        // AuthenticationController POST /forgot-password
            "/auth/verify-otp-password",    // AuthenticationController PUT /verify-otp-password (reset)

            // --- WebSocket ---
            "/ws/**",

            // --- OpenAPI / Swagger (dev only, restrict in prod profile) ---
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // --- Actuator (health only — restrict env/beans/heap in prod) ---
            "/actuator/health",

            // --- Shopping list share links (public) ---
            "/shopping-lists/shared/**",
    };

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint()))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
