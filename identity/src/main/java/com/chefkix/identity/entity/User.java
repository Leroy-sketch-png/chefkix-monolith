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
@Document(collection = "users") // Collection name in MongoDB
public class User {

  @Id String id; // Mongo ID is String (ObjectId as string)

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

  // Instead of @OneToOne, use @DBRef to reference another document
  @DBRef UserProfile userProfile;

  String googleId;

  String authProvider;

  // Instead of ManyToMany, use @DBRef to reference another collection
  @DBRef Set<Role> roles;
}
