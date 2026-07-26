package com.chefkix.culinary.features.pantry.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PantryRecipeMatchResponse {
    String recipeId;
    String recipeTitle;
    String coverImageUrl;
    int totalTimeMinutes;
    String difficulty;
    double matchPercentage;
    List<String> matchedIngredients;
    List<String> missingIngredients;
    List<String> expiringIngredientsUsed;
}
