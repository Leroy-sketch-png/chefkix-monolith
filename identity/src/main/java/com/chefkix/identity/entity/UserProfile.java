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
   */
  @Version Long version;

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
String accountType;
  String location;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  List<String> preferences;


  Statistics statistics;

  List<Friendship> friends;

  @Builder.Default
  boolean verified = false;

  @CreatedDate Instant createdAt;

  @LastModifiedDate Instant updatedAt;
}
