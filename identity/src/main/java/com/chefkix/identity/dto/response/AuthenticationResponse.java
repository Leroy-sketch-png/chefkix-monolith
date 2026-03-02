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
  String accessToken; // access token trả về từ Keycloak
  String refreshToken; // refresh token trả về từ Keycloak (optional, hoặc lưu httpOnly cookie)
  String idToken; // id token nếu cần
  String scope; // scope từ Keycloak
  boolean authenticated;
  LocalDateTime lastLogin;
  UserResponse user;
}
