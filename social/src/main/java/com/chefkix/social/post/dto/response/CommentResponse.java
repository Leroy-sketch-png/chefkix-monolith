package com.chefkix.social.post.dto.response;

import com.chefkix.social.post.entity.TaggedUserInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {
  String id; // Comment ID - required for FE operations (like, reply, delete)
  String userId;
  String postId;
  String displayName;
  String content;
  String avatarUrl;
  List<TaggedUserInfo> taggedUsers;

  @Builder.Default
  Integer likes = 0;
  @Builder.Default
  Integer replyCount = 0;
  
  /** Whether the current user has liked this comment */
  @JsonProperty("isLiked")
  @Builder.Default
  Boolean isLiked = false;

  @CreatedDate Instant createdAt;
  @LastModifiedDate Instant updatedAt;
}
