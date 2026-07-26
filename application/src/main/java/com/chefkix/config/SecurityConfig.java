package com.chefkix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    /**
     */
    private static final String[] PUBLIC_ENDPOINTS = {
"/auth/login",
"/auth/google",
"/auth/register",
"/auth/check-username",
"/auth/refresh-token",
"/auth/verify-otp",
"/auth/resend-otp",
"/auth/forgot-password",
"/auth/verify-otp-password",
"/auth/verify-otp-user",
            "/error",
            "/ws/**",

            "/actuator/health",

            "/shopping-lists/shared/**",

            "/search",
            "/search/autocomplete",
            "/search/trending",

            "/knowledge/**",
    };

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    /**
     */
    private static final String[] GUEST_GET_ENDPOINTS = {
"/recipes",
"/recipes/search",
"/recipes/trending",
"/recipes/*",
"/recipes/*/social-proof",
"/recipes/*/similar",
"/recipes/user/*",

"/posts/all",
"/posts/search",
"/posts/*",
"/posts/feed",
"/posts/*/comments",
"/posts/comments/*/replies",

"/auth/profile-only/*",
"/auth/profiles/paginated",
"/auth/leaderboard",

"/collections/featured",

"/achievements/user/*",
"/achievements",

"/challenges/today",
"/challenges/weekly",
"/challenges/community",
"/challenges/seasonal",

"/posts/reviews/recipe/*",
"/posts/reviews/recipe/*/stats",
"/posts/battles/active",

"/collections/*",
"/collections/*/posts",
"/collections/user/*",

"/knowledge-graph/**",
    };

    private String[] getPublicEndpoints() {
        if (swaggerEnabled) {
            List<String> all = new ArrayList<>(Arrays.asList(PUBLIC_ENDPOINTS));
            all.addAll(Arrays.asList(SWAGGER_ENDPOINTS));
            return all.toArray(new String[0]);
        }
        return PUBLIC_ENDPOINTS;
    }

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
                        .requestMatchers(getPublicEndpoints()).permitAll()
                        .requestMatchers(HttpMethod.GET, GUEST_GET_ENDPOINTS).permitAll()
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
                converter.setJwtGrantedAuthoritiesConverter(mergedAuthoritiesConverter(authoritiesConverter));
        return converter;
    }

        private Converter<Jwt, Collection<GrantedAuthority>> mergedAuthoritiesConverter(
                        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter) {
                return jwt -> {
                        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

                        Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
                        if (scopeAuthorities != null) {
                                authorities.addAll(scopeAuthorities);
                        }

                        authorities.addAll(extractRealmAuthorities(jwt));
                        authorities.addAll(extractClientAuthorities(jwt));

                        return authorities;
                };
        }

        private Collection<GrantedAuthority> extractRealmAuthorities(Jwt jwt) {
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess == null) {
                        return Collections.emptyList();
                }

                Object rolesClaim = realmAccess.get("roles");
                if (!(rolesClaim instanceof Collection<?> roles)) {
                        return Collections.emptyList();
                }

                return roles.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .map(SimpleGrantedAuthority::new)
                                .map(GrantedAuthority.class::cast)
                                .toList();
        }

        private Collection<GrantedAuthority> extractClientAuthorities(Jwt jwt) {
                Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                if (resourceAccess == null || resourceAccess.isEmpty()) {
                        return Collections.emptyList();
                }

                Set<GrantedAuthority> authorities = new LinkedHashSet<>();
                for (Object clientAccessObj : resourceAccess.values()) {
                        if (!(clientAccessObj instanceof Map<?, ?> clientAccess)) {
                                continue;
                        }

                        Object rolesClaim = clientAccess.get("roles");
                        if (!(rolesClaim instanceof Collection<?> roles)) {
                                continue;
                        }

                        for (Object roleObj : roles) {
                                if (roleObj instanceof String roleName) {
                                        authorities.add(new SimpleGrantedAuthority(roleName));
                                }
                        }
                }

                return authorities;
        }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
