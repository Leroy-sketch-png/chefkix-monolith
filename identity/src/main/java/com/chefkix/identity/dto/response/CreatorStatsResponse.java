package com.chefkix.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Creator Stats Response - Per vision_and_spec/03-social.txt
 * Shows recipe creator performance metrics
 *
 * Phase 1 (MVP): All fields implemented with real data
 * Phase 2 (Future): lastWeek data and change percentages require historical storage
 */
@Data
@Builder
public class CreatorStatsResponse {
    private long totalRecipesPublished;
    private long totalCooksOfYourRecipes;
    private long xpEarnedAsCreator;
    private Double avgRating; // Average rating across all creator's recipes
    private TopRecipeDto topRecipe;
    private WeeklyStatsDto thisWeek;
    // Phase 2: private WeeklyStatsDto lastWeek; // Requires XpResetScheduler to archive before reset
    private List<CreatorBadgeDto> creatorBadges;

    @Data
    @Builder
    public static class TopRecipeDto {
        private String id;
        private String title;
        private String coverImageUrl; // First image from recipe's coverImageUrl array (null if empty)
        private Integer cookTimeMinutes; // From recipe entity
        private String difficulty; // Beginner/Intermediate/Advanced/Expert (@JsonValue format)
        private long cookCount;
        private long xpGenerated;
        private Double averageRating; // Rating for this specific recipe
    }

    @Data
    @Builder
    public static class WeeklyStatsDto {
        private long newCooks;
        // Phase 2: private Double newCooksChange; // Requires lastWeek data
        private long xpEarned;
        // Phase 2: private Double xpEarnedChange; // Requires lastWeek data
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class CreatorBadgeDto {
        private String name;
        private String icon;
        private String recipeTitle; // Null if user-level badge
    }
}

