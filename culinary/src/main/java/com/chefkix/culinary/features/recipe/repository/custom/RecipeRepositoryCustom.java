package com.chefkix.culinary.features.recipe.repository.custom;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecipeRepositoryCustom {

    /**
     * Advanced search with Dynamic Filter
     */
    Page<Recipe> searchRecipes(RecipeSearchQuery query, Pageable pageable);

    /**
     * Increment viewCount asynchronously (Fire & Forget)
     */
    void incrementViewCount(String recipeId);

    /**
     * Safely update like/save count (Atomic Update)
     * Returns the latest Recipe to get the count for UI display
     * @param amount: +1 or -1
     */
    Recipe updateLikeCount(String recipeId, int amount);

    Recipe updateSaveCount(String recipeId, int amount);

    /**
     * Find published recipes with only fields needed for pantry/meal-plan ingredient matching.
     * Uses MongoDB projection to avoid loading heavy fields (steps, enrichment, etc.).
     */
    List<Recipe> findPublishedForIngredientMatching();
}