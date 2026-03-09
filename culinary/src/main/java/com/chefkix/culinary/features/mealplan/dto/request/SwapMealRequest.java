package com.chefkix.culinary.features.mealplan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Replace one meal slot in a plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwapMealRequest {
    String recipeId;     // null to clear slot
    @NotBlank
    String title;
    int totalTimeMinutes;
    int servings;
    boolean aiGenerated;
}
