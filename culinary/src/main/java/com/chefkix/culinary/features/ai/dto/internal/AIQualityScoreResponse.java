package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from AI service POST /api/v1/score_recipe_quality.
 * Contains quality score, tier, and dimensional breakdown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIQualityScoreResponse {
    private int overallScore;
    private String tier;
    private List<DimensionScore> dimensions;
    private List<StepScore> stepScores;
    private List<String> topSuggestions;
    private boolean publishReady;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DimensionScore {
        private String name;
        private int score;
        private String reason;
        private List<String> suggestions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepScore {
        private int stepNumber;
        private int score;
        private List<String> issues;
        private String suggestion;
    }
}
