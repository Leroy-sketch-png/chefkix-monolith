package com.chefkix.culinary.features.mealplan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwapMealRequest {
    @Size(max = 100)
String recipeId;
    @NotBlank
    @Size(max = 200)
    String title;
    int totalTimeMinutes;
    int servings;
    boolean aiGenerated;
}
