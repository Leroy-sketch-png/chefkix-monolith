package com.chefkix.identity.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "signup_requests")
public class SignupRequest {

  @Id String id;

  @NotBlank @Email @Size(max = 254) String email;
  @NotBlank @Size(min = 3, max = 30) String username;
  // Plain password - stored temporarily until OTP verification
  // Keycloak will hash it properly when user is created
  // This document is deleted after successful verification
  @NotBlank @Size(min = 6, max = 128) String password;

  // OTP fields
  String otpHash;
  Instant createdAt;
  Instant expiresAt;
  Instant lastOtpSentAt;
  Integer attempts;

  // Extra profile fields
  @NotBlank @Size(max = 50) String firstName;
  @NotBlank @Size(max = 50) String lastName;
  @Size(max = 100) String fullName;
  @Size(max = 50) String displayName;
  @Size(max = 20) String phoneNumber;
  @Size(max = 500) String avatarUrl;
  @Size(max = 500) String bio;
  @Size(max = 30) String accountType;
  @Size(max = 100) String location;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  @Size(max = 20) List<String> preferences;
}
