package com.chefkix.identity.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresenceResponse {
    String userId;
    String username;
    String displayName;
    String avatarUrl;
    boolean online;
    String activity; // "idle", "browsing", "cooking", "creating"
    String recipeTitle; // if cooking, what recipe
    long lastSeenEpoch; // epoch millis
}
