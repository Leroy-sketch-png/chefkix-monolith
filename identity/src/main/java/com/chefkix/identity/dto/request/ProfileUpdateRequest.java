package com.chefkix.identity.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO for profile update requests. All fields are optional — only non-null fields are applied.
 * Validation constraints prevent abuse (megabyte bios, unlimited preferences, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateRequest {

  // Personal info
  @Size(max = 50, message = "First name must be at most 50 characters")
  String firstName;

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

  @Size(max = 500, message = "Cover image URL must be at most 500 characters")
  String coverImageUrl;

  @Size(max = 500, message = "Bio must be at most 500 characters")
  String bio;

  @Size(max = 100, message = "Location must be at most 100 characters")
  String location;

  // Preferences & settings
  @Size(max = 20, message = "Preferences must contain at most 20 items")
  List<String> preferences;
}
