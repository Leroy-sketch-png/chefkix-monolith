package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
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

  String blockerId;

  String blockedId;

  @CreatedDate Instant createdAt;
}
