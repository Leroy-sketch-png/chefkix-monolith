package com.chefkix.culinary.api;

import com.chefkix.culinary.api.dto.CreatorInsightsInfo;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;

/**
 */
public interface RecipeProvider {

    /**
     *
     */
    CreatorInsightsInfo getCreatorInsights(String userId);

    /**
     *
     */
    RecipeSummaryInfo getRecipeSummary(String recipeId);

    RecipeSummaryInfo getPublicRecipeSummary(String recipeId);

    /**
     *
     */
    long cleanupDeletedUserData(String userId);
}
