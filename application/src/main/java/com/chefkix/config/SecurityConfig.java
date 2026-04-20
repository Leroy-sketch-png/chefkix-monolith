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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unified security configuration for the ChefKix monolith.
 * Merges public endpoints from all former microservices.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    /**
     * Public endpoints that do NOT require authentication.
     * CRITICAL: Must match ACTUAL controller @XxxMapping paths exactly.
     * Audited 2025-03 against all controllers in identity/notification modules.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // --- Auth (identity) -- pre-login flows only ---
            "/auth/login",                  // AuthenticationController POST /login
            "/auth/google",                 // AuthenticationController POST /google
            "/auth/register",               // AuthenticationController POST /register
            "/auth/check-username",         // AuthenticationController GET /check-username
            "/auth/refresh-token",          // AuthenticationController POST /refresh-token
            "/auth/verify-otp",             // OtpController POST /verify-otp (signup OTP)
            "/auth/resend-otp",             // OtpController POST /resend-otp
            "/auth/forgot-password",        // AuthenticationController POST /forgot-password
            "/auth/verify-otp-password",    // AuthenticationController PUT /verify-otp-password (reset)
            "/auth/verify-otp-user",        // ProfileController POST /verify-otp-user (signup finalize)
            "/error",
            // --- WebSocket ---
            "/ws/**",

            // --- Actuator (health only -- restrict env/beans/heap in prod) ---
            "/actuator/health",

            // --- Shopping list share links (public) ---
            "/shopping-lists/shared/**",

            // --- Typesense search + autocomplete (public -- typo-tolerant, no user data) ---
            "/search",
            "/search/autocomplete",
            "/search/trending",

            // --- Knowledge graph (public -- ingredient/technique lookups, no PII) ---
            "/knowledge/**",
    };

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    /**
     * Guest-browsable GET endpoints. Gate ACTIONS (like, save, comment, follow), not VIEWING.
     * Controllers already handle null/anonymous auth gracefully for these paths.
     */
    private static final String[] GUEST_GET_ENDPOINTS = {
            // --- Recipes (culinary module) ---
            "/recipes",                     // RecipeController GET / (search & filter)
            "/recipes/search",              // RecipeController GET /search (alias)
            "/recipes/trending",            // RecipeController GET /trending
            "/recipes/*",                   // RecipeController GET /{id} (recipe detail)
            "/recipes/*/social-proof",      // RecipeController GET /{id}/social-proof
            "/recipes/*/similar",           // RecipeController GET /{id}/similar
            "/recipes/user/*",              // RecipeController GET /user/{userId}

            // --- Posts/Feed (social module) ---
            "/posts/all",                   // PostController GET /all (global feed)
            "/posts/search",                // PostController GET /search
            "/posts/*",                     // PostController GET /{postId} (single post)
            "/posts/feed",                  // PostController GET /feed?userId= (user posts)
            "/posts/*/comments",            // CommentController GET /{postId}/comments (view comments)
            "/posts/comments/*/replies",    // CommentController GET /comments/{commentId}/replies

            // --- Public profiles (identity module) ---
            "/auth/profile-only/*",         // ProfileController GET /profile-only/{userId}
            "/auth/profiles/paginated",     // ProfileController GET /profiles/paginated (user discovery)
            "/auth/leaderboard",            // LeaderboardController GET /leaderboard

            // --- Featured collections (social module) ---
            "/collections/featured",        // CollectionController GET /featured (Season's Best)

            // --- Achievements (culinary module) ---
            "/achievements/user/*",         // AchievementController GET /user/{userId} (public skill tree)
            "/achievements",                // AchievementController GET / (full achievement catalog)

            // --- Challenges (culinary module) - browsable by guests ---
            "/challenges/today",            // ChallengeController GET /today (daily challenge)
            "/challenges/weekly",           // ChallengeController GET /weekly (weekly challenge)
            "/challenges/community",        // ChallengeController GET /community (community challenges)
            "/challenges/seasonal",         // ChallengeController GET /seasonal (seasonal events)

            // --- Reviews & Battles (social module) ---
            "/posts/reviews/recipe/*",      // PostController GET /reviews/recipe/{recipeId}
            "/posts/reviews/recipe/*/stats",// PostController GET /reviews/recipe/{recipeId}/stats
            "/posts/battles/active",        // PostController GET /battles/active

            // --- User collections (social module) ---
            "/collections/user/*",          // CollectionController GET /user/{userId} (public collections)

            // --- Knowledge graph (culinary module) ---
            "/knowledge-graph/**",          // KnowledgeGraphController GET (ingredients, techniques)
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
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
