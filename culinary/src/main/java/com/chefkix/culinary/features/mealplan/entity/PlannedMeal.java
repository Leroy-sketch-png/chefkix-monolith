package com.chefkix.culinary.features.mealplan.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlannedMeal {
String recipeId;
    String title;
    int totalTimeMinutes;
    int servings;
    boolean aiGenerated;
}
