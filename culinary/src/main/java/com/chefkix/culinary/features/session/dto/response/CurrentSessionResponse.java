package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.common.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CurrentSessionResponse {

    private String sessionId;
    private String recipeId;
private SessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer currentStep;
    private List<Integer> completedSteps;

    private List<CookingSession.ActiveTimer> activeTimers;

    private SessionRecipeInfo recipe;
    
private Integer baseXpAwarded;
private Integer pendingXp;
private Integer remainingXpAwarded;
    
    private String postId;
    private LocalDateTime postDeadline;
    private Integer daysRemaining;

    @Data
    @Builder
    public static class SessionRecipeInfo {
        private String id;
        private String title;
        private Integer totalSteps;
        private Integer xpReward;
        private List<String> coverImageUrl;
    }
}