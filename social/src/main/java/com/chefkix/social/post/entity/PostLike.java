package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "post_like")
@CompoundIndexes({
  @CompoundIndex(name = "post_user_idx", def = "{'postId': 1, 'userId': 1}", unique = true),
  @CompoundIndex(name = "user_post_idx", def = "{'userId': 1, 'postId': 1}"),
  @CompoundIndex(name = "user_recent_like_idx", def = "{'userId': 1, 'createdDate': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostLike {
  @Id String id;

  String postId;
  String userId;

  @CreatedDate LocalDateTime createdDate;
}
