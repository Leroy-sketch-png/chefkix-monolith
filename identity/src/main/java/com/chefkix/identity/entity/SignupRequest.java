package com.chefkix.identity.entity;

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

  String email;
  String username;
  // Plain password - stored temporarily until OTP verification
  // Keycloak will hash it properly when user is created
  // This document is deleted after successful verification
  String password;

  // OTP fields
  String otpHash;
  Instant createdAt;
  Instant expiresAt;
  Instant lastOtpSentAt;
  Integer attempts;

  // Extra profile fields
  String firstName;
  String lastName;
  String fullName;
  String displayName;
  String phoneNumber;
  String avatarUrl;
  String bio;
  String accountType;
  String location;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  List<String> preferences;
}
