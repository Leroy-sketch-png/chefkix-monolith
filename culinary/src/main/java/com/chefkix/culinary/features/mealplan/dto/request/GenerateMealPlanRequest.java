package com.chefkix.culinary.features.mealplan.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request to generate a meal plan via AI.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateMealPlanRequest {

    @Min(1)
    @Max(7)
    @Builder.Default
    int days = 7;

    MealPreferences preferences;

    /** Sent from FE or enriched from pantry on BE */
    List<String> pantryItems;

    /** Existing recipe IDs the AI should prefer */
    List<String> existingRecipeIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MealPreferences {
        List<String> dietary;
        List<String> cuisinePreferences;
        MaxTimePerMeal maxTimePerMeal;
        @Builder.Default
        int servings = 2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MaxTimePerMeal {
        @Builder.Default
        int breakfast = 15;
        @Builder.Default
        int lunch = 30;
        @Builder.Default
        int dinner = 60;
    }
}
