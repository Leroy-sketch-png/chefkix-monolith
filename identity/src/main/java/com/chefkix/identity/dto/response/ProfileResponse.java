package com.chefkix.identity.dto.response;

import com.chefkix.identity.enums.RelationshipStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
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
public class ProfileResponse {
  String profileId;
  String userId;

  // Auth info
  String email;
  String username;

  // Personal info
  String firstName;
  String lastName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  String displayName;
  String phoneNumber;
  String avatarUrl;
  String coverImageUrl;
  String bio;
  String accountType; // normal, chef, admin
  String location;

  // Preferences & settings
  List<String> preferences;

  // Gamification statistics
  StatisticResponse statistics;

  List<FriendshipResponse> friends;
  RelationshipStatus relationshipStatus;
  
  // Relationship fields - JSON names match FE expectations
  @JsonProperty("isFollowing")
  Boolean following;    // Current user follows this profile
  
  @JsonProperty("isFollowedBy")
  Boolean followedBy;   // This profile follows current user
  
  @JsonProperty("isBlocked")
  Boolean isBlocked;    // Current user has blocked this profile

  // Metadata
  Instant createdAt;
  Instant updatedAt;
}
