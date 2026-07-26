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

  String email;
  String username;

  String firstName;
  String lastName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  String displayName;
  String phoneNumber;
  String avatarUrl;
  String coverImageUrl;
  String bio;
String accountType;
  String location;

  List<String> preferences;

  StatisticResponse statistics;

  List<FriendshipResponse> friends;
  RelationshipStatus relationshipStatus;
  
  @JsonProperty("isFollowing")
Boolean following;
  
  @JsonProperty("isFollowedBy")
Boolean followedBy;
  
  @JsonProperty("isBlocked")
Boolean isBlocked;

  @JsonProperty("isVerified")
  boolean verified;

  Instant createdAt;
  Instant updatedAt;
}
