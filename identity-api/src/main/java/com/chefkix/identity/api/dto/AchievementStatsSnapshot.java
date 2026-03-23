package com.chefkix.identity.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Lightweight stats snapshot for cross-module achievement evaluation.
 * Only includes the fields needed by culinary module's AchievementService.
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
