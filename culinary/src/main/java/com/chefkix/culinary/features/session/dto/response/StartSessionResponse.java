package com.chefkix.culinary.features.session.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StartSessionResponse {
    private String sessionId;
    private String recipeId;
    private LocalDateTime startedAt; // Spring auto-formats to ISO-8601
    private String status;           // "in_progress"
    private Integer currentStep;
    private Integer totalSteps;
    private List<Object> activeTimers; // Empty array []

    private RecipeInfo recipe;

    @Data
    @Builder
    public static class RecipeInfo {
        private String id;
        private String title;
        private Integer xpReward;
        private Integer cookTimeMinutes;
    }
}