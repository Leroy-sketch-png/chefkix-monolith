package com.chefkix.culinary.features.recipe.repository.custom;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface RecipeRepositoryCustom {

    /**
     */
    Page<Recipe> searchRecipes(RecipeSearchQuery query, Pageable pageable);

    /**
     */
    void incrementViewCount(String recipeId);

    /**
     */
    Recipe updateLikeCount(String recipeId, int amount);

    Recipe updateSaveCount(String recipeId, int amount);

    /**
     */
    List<Recipe> findPublishedForIngredientMatching();

    List<Recipe> findChallengeCandidates(Map<String, Object> criteria, int limit);
}
