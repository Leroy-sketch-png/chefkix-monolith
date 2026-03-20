package com.chefkix.identity.config;

import com.chefkix.identity.utils.HttpOnlyCookieUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class CookieConfig {

    private final Environment environment;

    public CookieConfig(Environment environment) {
        this.environment = environment;
    }

    @Value("${app.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    @PostConstruct
    public void init() {
        boolean isProdProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));

        if (isProdProfile) {
            if (!secureCookies) {
                throw new IllegalStateException("app.cookie.secure must be true in production profile");
            }
            if (cookieDomain == null || cookieDomain.isBlank() || "localhost".equalsIgnoreCase(cookieDomain.trim())) {
                throw new IllegalStateException("app.cookie.domain must be a real domain in production profile");
            }
        }

        HttpOnlyCookieUtils.configure(cookieDomain, secureCookies);
    }
}
