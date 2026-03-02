package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Block entity - represents a user blocking another user. When user A blocks user B: - B cannot see
 * A's posts, recipes, comments - B cannot follow A - B cannot message A - A cannot see B's posts,
 * recipes, comments (mutual invisibility)
 */
@Document(collection = "blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@CompoundIndex(name = "unique_block", def = "{'blockerId': 1, 'blockedId': 1}", unique = true)
public class Block {
  @Id String id;

  /** The user who initiated the block */
  String blockerId;

  /** The user who is blocked */
  String blockedId;

  @CreatedDate Instant createdAt;
}
