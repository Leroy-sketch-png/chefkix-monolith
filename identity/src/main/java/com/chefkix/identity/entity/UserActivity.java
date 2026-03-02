package com.chefkix.identity.entity;

import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "user_activity") // Tên collection trong MongoDB
public class UserActivity {

  @Id String id; // ID trong Mongo là String (ObjectId dưới dạng chuỗi)

  String keycloakId;

  @Email String email;

  @Builder.Default Boolean enabled = true;

  LocalDateTime lastLogin;

  @Builder.Default Integer failedLoginCount = 0;

  LocalDateTime lockedUntil;

  String adminNote;

  @Builder.Default Boolean isOnline = false;
  LocalDateTime lastActive;

  @CreatedDate LocalDateTime createdAt;

  @LastModifiedDate LocalDateTime updatedAt;
}
