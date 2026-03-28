package com.chefkix.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.chefkix.notification.dto.request.EmailRequest;
import com.chefkix.notification.dto.response.EmailResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * External HTTP client for the Brevo transactional email API.
 * Replaces the old Feign-based EmailClient with Spring's RestClient.
 */
@Component
@Slf4j
public class BrevoEmailClient {

    private final RestClient restClient;
    private final boolean configured;

    public BrevoEmailClient(
            @Value("${notification.email.brevo-url}") String brevoUrl,
            @Value("${notification.email.brevo-apikey}") String apiKey) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl(brevoUrl)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    @PostConstruct
    public void validate() {
        if (!configured) {
            log.warn("=========================================================");
            log.warn("[BREVO] WARNING: BREVO_API_KEY is not set or empty.");
            log.warn("[BREVO] Email sending is DISABLED for this session.");
            log.warn("[BREVO] OTPs will be printed to the console log instead.");
            log.warn("[BREVO] Set BREVO_API_KEY in .env to enable real emails.");
            log.warn("=========================================================");
        } else {
            log.info("[BREVO] Email client initialized (api-key configured)");
        }
    }

    /** Returns true if the Brevo API key is configured and email sending is enabled. */
    public boolean isConfigured() {
        return configured;
    }

    public EmailResponse sendEmail(EmailRequest request) {
        return restClient
                .post()
                .uri("/v3/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmailResponse.class);
    }
}
