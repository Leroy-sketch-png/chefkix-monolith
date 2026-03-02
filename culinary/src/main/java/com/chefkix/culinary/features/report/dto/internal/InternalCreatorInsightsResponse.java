package com.chefkix.culinary.features.report.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Internal DTO for recipe-service → identity-service communication.
 * Used by StatisticsService.getMyCreatorStats() via Feign client.
 */
@Data
@Builder
public class InternalCreatorInsightsResponse {
    private TopRecipeDto topRecipe;
    private List<TopRecipeDto> highPerformingRecipes;
    private Double avgRating; // Average rating across all creator's recipes

    @Data
    @Builder
    public static class TopRecipeDto {
        private String id;
        private String title;
        private long cookCount;
        private long xpGenerated;
        // New fields for Creator Stats page
        private String coverImageUrl; // First image from recipe's coverImageUrl array
        private Integer cookTimeMinutes;
        private String difficulty; // Beginner/Intermediate/Advanced/Expert (@JsonValue format)
        private Double averageRating; // Rating for this specific recipe
    }
}