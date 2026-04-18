package com.chefkix.culinary.features.challenge.model;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Defines the structure of a Challenge.
 * This class exists only in memory for logic purposes, not persisted to DB.
 */
@Data
@Builder
public class ChallengeDefinition {

    // Unique identifier (e.g., "noodle-day", "quick-meal-1")
    // Stored in completion history log when user completes the challenge
    private String id;

    // Display name (e.g., "Noodle Day 🍜")
    private String title;

    // Short description
    private String description;

    // Bonus XP reward
    private int bonusXp;

    // Target completions (1 for daily, N for weekly)
    @Builder.Default
    private int target = 1;

    // Metadata for Frontend UI display
    // e.g., which tag to highlight, which icon to show
    // Example: { "cuisine": "Italian", "icon": "🍝", "color": "#FF5733" }
    private Map<String, Object> criteriaMetadata;

    // CORE LOGIC (Functional Interface)
    // Validates whether a Recipe satisfies the challenge requirements.
    // Input: Recipe -> Output: true/false
    private Predicate<Recipe> validationLogic;

    /**
     * Helper method for quick validation.
     * @param recipe The recipe the user just cooked
     * @return true if the challenge is satisfied
     */
    public boolean isSatisfiedBy(Recipe recipe) {
        if (validationLogic == null) return false;
        return validationLogic.test(recipe);
    }
}