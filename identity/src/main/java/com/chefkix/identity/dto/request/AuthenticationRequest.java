package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
  @NotBlank(message = "Email or username is required")
  @Size(max = 255, message = "Email or username must not exceed 255 characters")
  String emailOrUsername;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
  String password;
}
