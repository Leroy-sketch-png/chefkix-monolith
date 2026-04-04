package com.chefkix.culinary.features.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wraps a recipe recommendation with transparency metadata.
 * Used by Tonight's Pick and future recommendation surfaces.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private RecipeDetailResponse recipe;
    private String whyRecommended;
    private List<String> matchSignals;
    private Double confidenceScore;
}
