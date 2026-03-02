package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {
  @Id String id;
  String userId;
  String postId;
  String displayName;
  String content;
  String avatarUrl;

  @Builder.Default Integer likes = 0;
  @Builder.Default Integer replyCount = 0;

  @CreatedDate Instant createdAt;
  @LastModifiedDate Instant updatedAt;

  List<String> taggedUserIds;
}
