package com.chefkix.identity.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "user_profiles")
public class UserProfile {

  @Id String id;

  /**
   * Optimistic locking version — enables @Retryable(OptimisticLockingFailureException)
   * in StatisticsService to detect concurrent writes (XP rewards, follower counts, etc.).
   */
  @Version Long version;

  /** Reference to Keycloak User ID (or User table in a separate DB if self-managed). */
  @Indexed(unique = true) String userId;

  @TextIndexed(weight = 10)
  String displayName;
  @Indexed(unique = true) @TextIndexed(weight = 8)
  String username;
  @Indexed(unique = true) String email;
  @TextIndexed(weight = 5)
  String firstName;
  @TextIndexed(weight = 5)
  String lastName;
  String fullName;
  String phoneNumber;
  String avatarUrl;
  String coverImageUrl;
  @TextIndexed(weight = 3)
  String bio;
  String accountType; // normal, chef, admin...
  String location;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  /** List of preferences: e.g. ["vegan", "spicy", "asian-food"] */
  List<String> preferences;

  /** Or store additional custom settings as a map */
  // Map<String, Object> settings;

  /** Count fields should be updated by service or calculated via aggregation */
  Statistics statistics;

  List<Friendship> friends;

  /** Verified creator badge — paid feature, approved by admin */
  @Builder.Default
  boolean verified = false;

  @CreatedDate Instant createdAt;

  @LastModifiedDate Instant updatedAt;
}
