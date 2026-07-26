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
private LocalDateTime startedAt;
private String status;
    private Integer currentStep;
    private Integer totalSteps;
private List<Object> activeTimers;

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