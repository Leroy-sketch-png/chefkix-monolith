package com.chefkix.culinary.features.mealplan.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One day's 3-meal plan.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlannedDay {
    String dayOfWeek; // "Monday", "Tuesday", etc.
    PlannedMeal breakfast;
    PlannedMeal lunch;
    PlannedMeal dinner;
}
