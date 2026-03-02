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

    // --- CÁC TRƯỜNG CHÍNH ---
    private String sessionId;
    private String recipeId;
    private SessionStatus status; // "in_progress", "paused", "completed", "posted"
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // --- TIẾN ĐỘ ---
    private Integer currentStep;
    private List<Integer> completedSteps;

    // --- TIMER (Đã tính toán) ---
    private List<CookingSession.ActiveTimer> activeTimers;

    // --- RECIPE SNAPSHOT ---
    private SessionRecipeInfo recipe;
    
    // --- XP TRACKING (set after completion) ---
    private Integer baseXpAwarded;  // 30% XP awarded immediately at completion
    private Integer pendingXp;      // 70% XP pending until post is created
    private Integer remainingXpAwarded; // XP actually awarded when post was linked
    
    // --- POST LINKING ---
    private String postId;
    private LocalDateTime postDeadline;
    private Integer daysRemaining;

    // --- INNER DTO CHO RECIPE SNAPSHOT ---
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