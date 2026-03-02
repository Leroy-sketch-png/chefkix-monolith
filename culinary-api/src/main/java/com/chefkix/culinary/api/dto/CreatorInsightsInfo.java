package com.chefkix.culinary.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Creator insights/stats for a user's recipe portfolio.
 * <p>
 * Unifies: identity's {@code InternalCreatorInsightsResponse} and
 * recipe-service's {@code InternalCreatorInsightsResponse} (which had a different inner class structure).
 * <p>
 * Also incorporates fields from identity's {@code CreatorStatsResponse} used for the profile page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatorInsightsInfo {

    int totalRecipesPublished;

    long totalCooksOfYourRecipes;

    double avgRating;

    double xpEarnedAsCreator;

    List<TopRecipeInfo> topRecipes;

    List<TopRecipeInfo> highPerformingRecipes;

    WeeklyStats weeklyStats;

    List<CreatorBadge> creatorBadges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopRecipeInfo {
        String id;
        String title;
        String coverImageUrl;
        int cookTimeMinutes;
        String difficulty;
        long cookCount;
        double xpGenerated;
        double averageRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WeeklyStats {
        long newCooks;
        double xpEarned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreatorBadge {
        String name;
        String icon;
        String recipeTitle;
    }
}
