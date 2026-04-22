package com.chefkix.culinary.api;

import com.chefkix.culinary.api.dto.CreatorInsightsInfo;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;

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

    /**
     * Get lightweight recipe info for cross-module display (battle cards, review headers).
     *
     * @param recipeId the recipe's ID
     * @return summary info (id, title, coverImage, author) or null if not found
     */
    RecipeSummaryInfo getRecipeSummary(String recipeId);

    /**
     * Archive or remove culinary-module data after a user account is deleted.
     * Implementations may mix archival and personal-state deletion depending on data semantics.
     *
     * @param userId deleted user ID
     * @return number of affected records
     */
    long cleanupDeletedUserData(String userId);
}
