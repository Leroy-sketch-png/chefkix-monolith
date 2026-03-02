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

import java.lang.reflect.Field;
import java.util.List;

/**
 * Keycloak Admin REST API client.
 * <p>
 * Replaces the old {@code @FeignClient(name="identity-client", url="${idp.url}")} since
 * Spring Cloud OpenFeign is not available in the monolith.
 * Uses WebClient (already a dependency for KeycloakService).
 */
@Slf4j
@Component
public class KeycloakAdminClient {

    private final WebClient webClient;

    public KeycloakAdminClient(@Value("${idp.url}") String keycloakUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(keycloakUrl)
                .build();
    }

    /**
     * Exchange credentials for tokens (password grant or refresh_token grant).
     * Replaces: {@code @PostMapping("/realms/nottisn/protocol/openid-connect/token")}
     */
    public TokenExchangeResponse exchangeToken(TokenExchangeParam param) {
        MultiValueMap<String, String> formData = toFormData(param);

        return webClient.post()
                .uri("/realms/nottisn/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenExchangeResponse.class)
                .block();
    }

    /**
     * Create a user in Keycloak.
     * Replaces: {@code @PostMapping("/admin/realms/nottisn/users")}
     */
    public ResponseEntity<?> createUser(String adminToken, UserCreationParam param) {
        return webClient.post()
                .uri("/admin/realms/nottisn/users")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(param)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    /**
     * Trigger actions email (e.g., VERIFY_EMAIL).
     * Replaces: {@code @PostMapping("/admin/realms/{realm}/users/{userId}/execute-actions-email")}
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
                .block();
    }

    /**
     * Reset a user's password in Keycloak.
     * Replaces: {@code @PutMapping("/admin/realms/nottisn/users/{userId}/reset-password")}
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
                .block();
    }

    /**
     * Convert a POJO's fields to form data (replicates Feign's @QueryMap behavior for
     * application/x-www-form-urlencoded bodies).
     */
    private MultiValueMap<String, String> toFormData(Object param) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        for (Field field : param.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(param);
                if (value != null) {
                    // Use snake_case field name from @JsonProperty if present,
                    // otherwise use the Java field name
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
