package com.chefkix.identity.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
  String accessToken; // access token returned from Keycloak
  String refreshToken; // refresh token returned from Keycloak (optional, or stored in httpOnly cookie)
  String idToken; // id token if needed
  String scope; // scope from Keycloak
  boolean authenticated;
  LocalDateTime lastLogin;
  UserResponse user;
}
