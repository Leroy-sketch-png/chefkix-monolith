package com.chefkix.identity.service;

import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class KeycloakService {

  @Value("${idp.client-secret}")
  @NonFinal
  String clientSecret;

  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public KeycloakService(
      WebClient.Builder webClientBuilder,
      @Value("${keycloak.auth-server-url:http://localhost:8180}") String keycloakAuthServerUrl) {

    // Configure dynamic Base URL based on environment variable
    this.webClient =
        webClientBuilder
            .baseUrl(keycloakAuthServerUrl + "/realms/nottisn/protocol/openid-connect")
            .build();
  }

  public TokenExchangeResponse login(String usernameOrEmail, String password) {
    log.debug(">>> [KEYCLOAK] Attempting login for user: {}", usernameOrEmail);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", "chefkix-be");
    body.add("client_secret", clientSecret);
    body.add("username", usernameOrEmail);
    body.add("password", password);
    body.add("scope", "openid");

    try {
      return webClient
          .post()
          .uri("/token")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .bodyValue(body)
          .exchangeToMono(
              response -> {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        respBody -> {
                          // Log response status for debugging
                          log.debug(">>> [KEYCLOAK] Response status: {}", response.statusCode());

                          if (response.statusCode().isError()) {
                            log.warn(
                                ">>> [KEYCLOAK] Login failed for user '{}'. Error: {}",
                                usernameOrEmail,
                                respBody);
                            return Mono.error(new AppException(ErrorCode.INVALID_CREDENTIALS));
                          }

                          try {
                            TokenExchangeResponse tokenResponse =
                                objectMapper.readValue(respBody, TokenExchangeResponse.class);
                            log.debug(
                                ">>> [KEYCLOAK] Login successful for user: {}", usernameOrEmail);
                            return Mono.just(tokenResponse);
                          } catch (Exception e) {
                            log.error(">>> [KEYCLOAK] Failed to parse token response", e);
                            return Mono.error(
                                new AppException(ErrorCode.INVALID_CREDENTIALS));
                          }
                        });
              })
          .block();

    } catch (Exception e) {
      log.error(
          ">>> [KEYCLOAK] Login exception for user '{}': {}", usernameOrEmail, e.getMessage());
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }
  }

  public TokenExchangeResponse refreshToken(String refreshToken) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "refresh_token");
    body.add("client_id", "chefkix-be");
    body.add("client_secret", clientSecret);
    body.add("refresh_token", refreshToken);

    try {
      return webClient
          .post()
          .uri("/token")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(TokenExchangeResponse.class)
          .block();
    } catch (WebClientResponseException e) {
      log.warn("Token refresh failed: status={}", e.getStatusCode());
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  public void logout(String refreshToken) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", "chefkix-be");
    body.add("client_secret", clientSecret);
    body.add("refresh_token", refreshToken);

    try {
      webClient
          .post()
          .uri("/logout")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    } catch (WebClientResponseException e) {
      log.warn("Keycloak logout failed: status={}", e.getStatusCode());
    }
  }

    /**
     * Attempts to log in to Keycloak to verify the password using WebClient.
     */
    public boolean verifyPassword(String username, String rawPassword) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", "chefkix-be");
        if (clientSecret != null && !clientSecret.isEmpty()) {
            formData.add("client_secret", clientSecret);
        }
        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", rawPassword);

        try {
            // FIXED: Just use "/token" because baseUrl already has the rest of the path!
            webClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true; // Password is correct

        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.BadRequest e) {
            log.warn("Password verification failed for user: {}", username);
            return false;
        } catch (Exception e) {
            log.error("Error communicating with Keycloak for password verification", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
