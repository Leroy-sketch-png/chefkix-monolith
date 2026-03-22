package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO for the Python AI service /api/v1/generate_meal_plan endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIMealPlanRequest {

    List<String> pantryItems;
    Preferences preferences;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Preferences {
        List<String> dietaryTags;
        Integer maxTimeMinutes;
        Integer servings;
        @Builder.Default
        int daysToGenerate = 7;
        List<String> cuisinePreferences;
        String skillLevel;
    }
}
