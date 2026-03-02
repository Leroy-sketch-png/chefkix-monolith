package com.chefkix.identity.exception;

import com.chefkix.identity.dto.identity.KeyCloakError;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class ErrorNormalizer {
  private final ObjectMapper objectMapper;
  private final Map<String, ErrorCode> errorCodeMap;

  public ErrorNormalizer() {
    objectMapper = new ObjectMapper();
    errorCodeMap = new HashMap<>();

    errorCodeMap.put("User exists with same username", ErrorCode.USER_EXISTED);
    errorCodeMap.put("User exists with same email", ErrorCode.EMAIL_EXISTED);
    errorCodeMap.put("User name is missing", ErrorCode.USERNAME_IS_MISSING);
  }

  public AppException handleKeyCloakException(Exception exception) {
    try {
      log.warn("Cannot complete request", exception);

      // Extract response body: WebClientResponseException (monolith) replaces FeignException
      String body = "";
      if (exception instanceof WebClientResponseException wcre) {
        body = wcre.getResponseBodyAsString();
      } else {
        body = exception.getMessage();
      }

      var response = objectMapper.readValue(body, KeyCloakError.class);

      if (Objects.nonNull(response.getErrorMessage())
          && Objects.nonNull(errorCodeMap.get(response.getErrorMessage()))) {
        return new AppException(errorCodeMap.get(response.getErrorMessage()));
      }
    } catch (JsonProcessingException e) {
      log.error("Cannot deserialize content", e);
    }

    return new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
  }
}
