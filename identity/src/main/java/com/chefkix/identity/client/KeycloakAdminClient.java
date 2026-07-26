package com.chefkix.identity.client;

import com.chefkix.identity.dto.identity.ResetPasswordParam;
import com.chefkix.identity.dto.identity.TokenExchangeParam;
import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.identity.dto.identity.UserCreationParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 */
@Slf4j
@Component
public class KeycloakAdminClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public KeycloakAdminClient(@Value("${idp.url}") String keycloakUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(keycloakUrl)
                .build();
    }

    /**
     */
    public TokenExchangeResponse exchangeToken(TokenExchangeParam param) {
        MultiValueMap<String, String> formData = toFormData(param);

        return webClient.post()
                .uri("/realms/nottisn/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenExchangeResponse.class)
            .timeout(REQUEST_TIMEOUT)
                .block();
    }

    /**
     */
    public ResponseEntity<?> createUser(String adminToken, UserCreationParam param) {
        return webClient.post()
                .uri("/admin/realms/nottisn/users")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(param)
                .retrieve()
                .toBodilessEntity()
            .timeout(REQUEST_TIMEOUT)
                .block();
    }

    /**
     */
    public ResponseEntity<Void> executeActionsEmail(String bearerToken, String realm,
                                                     String userId, List<String> actions) {
        return webClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/execute-actions-email", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(actions)
                .retrieve()
                .toBodilessEntity()
            .timeout(REQUEST_TIMEOUT)
                .block();
    }

    /**
     */
    public ResponseEntity<?> resetPassword(String bearerToken, String userId,
                                            ResetPasswordParam param) {
        return webClient.put()
                .uri("/admin/realms/nottisn/users/{userId}/reset-password", userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(param)
                .retrieve()
                .toBodilessEntity()
            .timeout(REQUEST_TIMEOUT)
                .block();
    }

    public ResponseEntity<?> enableUser(String bearerToken, String userId) {
        return webClient.put()
                .uri("/admin/realms/nottisn/users/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("enabled", true))
                .retrieve()
                .toBodilessEntity()
                .timeout(REQUEST_TIMEOUT)
                .block();
    }

    /**
     */
    public void logoutUser(String bearerToken, String userId) {
        try {
            webClient.post()
                    .uri("/admin/realms/nottisn/users/{userId}/logout", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            log.info("All sessions revoked for Keycloak user {}", userId);
        } catch (Exception e) {
            log.error("Failed to revoke sessions for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     */
    public void deleteUser(String bearerToken, String userId) {
        webClient.delete()
                .uri("/admin/realms/nottisn/users/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .toBodilessEntity()
                .timeout(REQUEST_TIMEOUT)
                .block();
        log.info("Deleted Keycloak user {}", userId);
    }

    /**
     */
    private MultiValueMap<String, String> toFormData(Object param) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        for (Field field : param.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(param);
                if (value != null) {
                    String name = field.getName();
                    var jsonProp = field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
                    if (jsonProp != null && !jsonProp.value().isEmpty()) {
                        name = jsonProp.value();
                    }
                    formData.add(name, value.toString());
                }
            } catch (IllegalAccessException e) {
                log.warn("Failed to read field {} for form data: {}", field.getName(), e.getMessage());
            }
        }
        return formData;
    }
}
