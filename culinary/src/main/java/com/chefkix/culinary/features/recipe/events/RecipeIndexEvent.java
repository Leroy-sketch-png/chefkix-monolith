package com.chefkix.culinary.features.recipe.events;

import com.chefkix.culinary.features.recipe.entity.Recipe;

/**
 *
 *
 */
public record RecipeIndexEvent(Recipe recipe, String action, String recipeId) {

    public static RecipeIndexEvent index(Recipe recipe) {
        return new RecipeIndexEvent(recipe, "INDEX", recipe.getId());
    }

    public static RecipeIndexEvent remove(String recipeId) {
        return new RecipeIndexEvent(null, "REMOVE", recipeId);
    }
}
