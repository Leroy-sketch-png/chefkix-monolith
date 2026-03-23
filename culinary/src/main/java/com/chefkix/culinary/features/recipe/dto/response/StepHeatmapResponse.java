package com.chefkix.culinary.features.recipe.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Step-level cooking analytics for creators.
 * Aggregated from all completed CookingSessions for a given recipe.
 * Powers the "42% of cooks struggle at step 7" heatmap.
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

        /** % of sessions where this step was completed (0-100) */
        double completionRate;

        /** % of sessions where the timer for this step was skipped (0-100) */
        double skipRate;

        /** Average time spent on this step in seconds (from timer start to complete) */
        Double avgTimeSeconds;

        /** Estimated time from recipe definition (seconds) */
        Integer estimatedTimeSeconds;

        /** Whether this step is a "struggle point" — high skip or low completion */
        boolean strugglePoint;

        /** Drop-off = sessions that were abandoned AT this step */
        int abandonedAtCount;
    }
}
