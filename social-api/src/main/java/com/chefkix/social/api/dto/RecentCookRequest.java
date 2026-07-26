package com.chefkix.social.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecentCookRequest {
    String userId;
    String sessionId;
    String recipeId;
    String recipeTitle;
    String coverImageUrl;
    int durationMinutes;
    String displayName;
    String avatarUrl;
}
