package com.chefkix.culinary.features.recipe.events;

import com.chefkix.culinary.features.recipe.entity.Recipe;

/**
 * Spring Application Event fired after a recipe lifecycle change that requires
 * Typesense re-indexing. Uses the application event bus (same JVM) — no Kafka needed.
 *
 * Consumed by TypesenseDataSyncer in the application module.
 *
 * action = "INDEX"  → upsert recipe document in Typesense
 * action = "REMOVE" → delete recipe document from Typesense
 */
public record RecipeIndexEvent(Recipe recipe, String action, String recipeId) {

    /** Index a just-published recipe. */
    public static RecipeIndexEvent index(Recipe recipe) {
        return new RecipeIndexEvent(recipe, "INDEX", recipe.getId());
    }

    /** Remove a recipe that was archived/deleted. */
    public static RecipeIndexEvent remove(String recipeId) {
        return new RecipeIndexEvent(null, "REMOVE", recipeId);
    }
}
