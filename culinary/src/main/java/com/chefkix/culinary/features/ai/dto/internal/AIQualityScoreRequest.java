package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for AI service POST /api/v1/score_recipe_quality.
 * Sent by Java monolith during publish flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIQualityScoreRequest {
    private String title;
    private String description;
    private String difficulty;
    private List<Map<String, Object>> steps;
    private List<Map<String, Object>> ingredients;
    private int prepTimeMinutes;
    private int cookTimeMinutes;
    private boolean hasCoverImage;
    private int hasStepImages;
    private boolean hasVideo;
}
