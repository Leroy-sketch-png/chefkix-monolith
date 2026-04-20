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
  @Size(max = 50, message = "First name must be at most 50 characters")
  String firstName;

  @NotBlank(message = "LAST_NAME_REQUIRED")
  @Size(max = 50, message = "Last name must be at most 50 characters")
  String lastName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  @Size(max = 50, message = "Display name must be at most 50 characters")
  String displayName;

  @Size(max = 20, message = "Phone number must be at most 20 characters")
  String phoneNumber;

  @Size(max = 500, message = "Avatar URL must be at most 500 characters")
  String avatarUrl;

  @Size(max = 500, message = "Bio must be at most 500 characters")
  String bio;

  @Size(max = 20, message = "Account type must be at most 20 characters")
  String accountType; // normal, chef — validated in service layer

  @Size(max = 100, message = "Location must be at most 100 characters")
  String location;

  // Preferences & settings
  @Size(max = 20, message = "Maximum 20 preferences")
  List<String> preferences;
}
