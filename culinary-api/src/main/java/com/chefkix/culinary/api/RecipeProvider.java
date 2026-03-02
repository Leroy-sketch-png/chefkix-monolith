package com.chefkix.culinary.api;

import com.chefkix.culinary.api.dto.CreatorInsightsInfo;

/**
 * Cross-module contract for recipe-related read operations.
 * <p>
 * Implemented by {@code culinary} module, consumed by {@code identity} module.
 * Replaces: RecipeClient Feign client in identity-service.
 */
public interface RecipeProvider {

    /**
     * Get creator insights/stats for a user's published recipes.
     * Replaces: {@code GET /api/v1/internal/recipes/creator-insights/{userId}}
     * (recipe-service's internal endpoint consumed by identity-service).
     *
     * @param userId the creator's user ID
     * @return insights about the user's recipe portfolio
     */
    CreatorInsightsInfo getCreatorInsights(String userId);
}
