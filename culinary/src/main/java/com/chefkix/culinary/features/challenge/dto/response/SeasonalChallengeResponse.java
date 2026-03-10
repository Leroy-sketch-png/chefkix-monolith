package com.chefkix.culinary.features.challenge.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
 * Response for GET /challenges/seasonal.
 * Shows active/upcoming seasonal events with per-user progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeasonalChallengeResponse {

    String id;
    String title;
    String description;
    String emoji;

    // Theme
    String theme;
    String heroImageUrl;
    String accentColor;

    // Goal (per user)
    int targetCount;
    String targetUnit;

    // Reward
    int rewardXp;
    String rewardBadgeId;
    String rewardBadgeName;

    // Scheduling
    String startsAt; // ISO8601
    String endsAt;   // ISO8601
    String status;   // UPCOMING, ACTIVE, COMPLETED, EXPIRED

    // User progress
    int userProgress;       // how many qualifying recipes this user has completed
    boolean userCompleted;  // whether user has earned the reward
    String userCompletedAt; // ISO8601, null if not completed

    // Criteria
    Map<String, Object> criteria;

    // Curated recipes
    List<ChallengeResponse.RecipePreviewDto> featuredRecipes;

    // Tags
    List<String> tags;
}
