package com.chefkix.identity.entity;

import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "users") // Tên collection trong MongoDB
public class User {

  @Id String id; // ID trong Mongo là String (ObjectId dưới dạng chuỗi)

  String username;

  @Email String email;

  String passwordHash;

  @Builder.Default Boolean enabled = true;

  LocalDateTime lastLogin;

  @Builder.Default Integer failedLoginCount = 0;

  LocalDateTime lockedUntil;

  String adminNote;

  @CreatedDate LocalDateTime createdAt;

  @LastModifiedDate LocalDateTime updatedAt;

  // Thay vì @OneToOne, dùng @DBRef nếu muốn liên kết sang document khác
  @DBRef UserProfile userProfile;

  String googleId;

  String authProvider;

  // Thay vì ManyToMany, dùng @DBRef để tham chiếu đến collection khác
  @DBRef Set<Role> roles;
}
