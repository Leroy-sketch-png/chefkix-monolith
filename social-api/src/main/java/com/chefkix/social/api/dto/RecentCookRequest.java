package com.chefkix.social.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for auto-creating a RECENT_COOK post after cooking session completion.
 * Used by culinary module via PostProvider cross-module call.
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
