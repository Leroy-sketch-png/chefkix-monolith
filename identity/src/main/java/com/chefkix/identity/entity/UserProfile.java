package com.chefkix.identity.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
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

  /** Tham chiếu đến Keycloak User ID (hoặc bảng User trong DB riêng nếu bạn tự quản lý). */
  @Indexed(unique = true) String userId;

  String displayName;
  @Indexed(unique = true) String username;
  @Indexed(unique = true) String email;
  String firstName;
  String lastName;
  String fullName;
  String phoneNumber;
  String avatarUrl;
  String coverImageUrl;
  String bio;
  String accountType; // normal, chef, admin...
  String location;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  /** Danh sách sở thích: ex: ["vegan", "spicy", "asian-food"] */
  List<String> preferences;

  /** Hoặc lưu thêm custom settings dưới dạng map */
  // Map<String, Object> settings;

  /** Các trường "count" nên được cập nhật bởi service hoặc tính bằng aggregation */
  Statistics statistics;

  List<Friendship> friends;

  @CreatedDate Instant createdAt;

  @LastModifiedDate Instant updatedAt;
}
