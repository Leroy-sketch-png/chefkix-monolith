package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  String email;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  String username;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
  String password;

  //    @Size(max = 100, message = "Display name cannot exceed 100 characters")
  //    String displayName;
}
