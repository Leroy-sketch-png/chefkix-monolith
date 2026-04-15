package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleAuthenticationRequest {
  @NotBlank(message = "Authorization code is required")
  String code;

  @NotBlank(message = "Redirect URI is required")
  String redirectUri;

  @NotBlank(message = "Code verifier is required")
  String codeVerifier;
}