package com.chefkix.identity.dto.response;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockResponse {
  String id;
  String blockedUserId;
  String blockedUsername;
  String blockedDisplayName;
  String blockedAvatarUrl;
  Instant blockedAt;
}
