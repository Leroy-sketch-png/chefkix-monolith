package com.chefkix.identity.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AchievementStatsSnapshot {
    int streakCount;
    long followerCount;
    long totalRecipesPublished;
}
