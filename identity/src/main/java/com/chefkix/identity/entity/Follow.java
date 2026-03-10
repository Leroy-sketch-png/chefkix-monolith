package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "follow")
@CompoundIndex(name = "follower_following_idx", def = "{'followerId': 1, 'followingId': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Follow {
  @Id String id;
  String followerId;
  @Indexed String followingId;
  @CreatedDate Instant createdAt;
}
