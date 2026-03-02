package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegistrationRequest {

  // Auth info
  @NotBlank(message = "USERNAME_REQUIRED")
  @Size(min = 4, max = 20, message = "INVALID_USERNAME")
  String username;

  @NotBlank(message = "PASSWORD_REQUIRED")
  @Size(min = 6, message = "INVALID_PASSWORD")
  String password;

  @NotBlank(message = "EMAIL_REQUIRED")
  @Email(message = "INVALID_EMAIL")
  String email;

  // Personal info - Required for Keycloak user creation
  @NotBlank(message = "FIRST_NAME_REQUIRED")
  String firstName;

  @NotBlank(message = "LAST_NAME_REQUIRED")
  String lastName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  String displayName;
  String phoneNumber;
  String avatarUrl;
  String bio;
  String accountType; // normal, chef, admin...
  String location;

  // Preferences & settings
  List<String> preferences;
}
