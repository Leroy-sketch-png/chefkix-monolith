package com.chefkix.culinary.features.recipe.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Per-recipe performance metrics for the creator dashboard.
 * Spec: vision_and_spec/21-creator-analytics.txt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatorPerformanceResponse {

    List<RecipePerformanceItem> recipes;
    CreatorSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecipePerformanceItem {
        String id;
        String title;
        List<String> coverImageUrl;
        String difficulty;
        int xpReward;
        long cookCount;
        long masteredByCount;
        Double averageRating;
        Integer creatorXpEarned;
        long likeCount;
        long saveCount;
        long viewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreatorSummary {
        int totalRecipes;
        long totalCooks;
        long totalViews;
        long totalLikes;
        double averageRating;
    }
}
