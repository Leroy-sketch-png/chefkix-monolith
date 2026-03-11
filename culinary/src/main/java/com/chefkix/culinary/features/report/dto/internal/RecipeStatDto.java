package com.chefkix.culinary.features.report.dto.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Lightweight DTO for MongoDB aggregation results in trending score calculations.
 * Used by {@link com.chefkix.culinary.common.scheduled.RecipeScheduled}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeStatDto {
    String recipeId;
    long count;
}
