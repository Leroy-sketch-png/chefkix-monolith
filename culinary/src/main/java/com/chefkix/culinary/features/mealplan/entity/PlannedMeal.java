package com.chefkix.culinary.features.mealplan.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Single meal slot inside a planned day.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlannedMeal {
    String recipeId;        // null if AI-generated suggestion (not yet in DB)
    String title;
    int totalTimeMinutes;
    int servings;
    boolean aiGenerated;
}
