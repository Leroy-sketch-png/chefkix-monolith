package com.chefkix.culinary.features.recipe.dto.response;

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
public class StepHeatmapResponse {

    String recipeId;
    String recipeTitle;
    int totalSessions;
    List<StepAnalytics> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StepAnalytics {
        int stepNumber;
        String title;

        double completionRate;

        double skipRate;

        Double avgTimeSeconds;

        Integer estimatedTimeSeconds;

        boolean strugglePoint;

        int abandonedAtCount;
    }
}
