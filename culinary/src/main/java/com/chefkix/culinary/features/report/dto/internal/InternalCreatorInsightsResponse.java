package com.chefkix.culinary.features.report.dto.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Internal DTO for creator insights within the culinary module.
 * <p>
 * Used by {@code RecipeService.getRecipeWithAboveTenCooks()} and mapped
 * to the cross-module {@code CreatorInsightsInfo} SPI DTO by {@code RecipeProviderImpl}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalCreatorInsightsResponse {

    TopRecipeDto topRecipe;

    List<TopRecipeDto> highPerformingRecipes;

    Double avgRating;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopRecipeDto {
        String id;
        String title;
        long cookCount;
        double xpGenerated;
        String coverImageUrl;
        int cookTimeMinutes;
        String difficulty;
        double averageRating;
    }
}
