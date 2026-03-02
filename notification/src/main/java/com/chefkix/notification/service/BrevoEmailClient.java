package com.chefkix.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.chefkix.notification.dto.request.EmailRequest;
import com.chefkix.notification.dto.response.EmailResponse;

/**
 * External HTTP client for the Brevo transactional email API.
 * Replaces the old Feign-based EmailClient with Spring's RestClient.
 */
@Component
public class BrevoEmailClient {

    private final RestClient restClient;

    public BrevoEmailClient(
            @Value("${notification.email.brevo-url}") String brevoUrl,
            @Value("${notification.email.brevo-apikey}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(brevoUrl)
                .defaultHeader("api-key", apiKey)
                .build();
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
