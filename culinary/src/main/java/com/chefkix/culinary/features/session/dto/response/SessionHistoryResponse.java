package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.culinary.common.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SessionHistoryResponse {
    private List<SessionItemDto> sessions;

    @Data
    @Builder
    public static class SessionItemDto {
        private String sessionId;
        private String recipeId;
        private String recipeTitle;
        private List<String> coverImageUrl; // Fetched from Recipe
        private SessionStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        // XP data - using Integer for clean game system values
        private Integer baseXpAwarded;
        private Integer pendingXp;
        private Integer xpEarned; // Total XP (base + remaining) if status=posted

        // Link Post
        private String postId;

        // Deadline (Only needs calculation for items with status=completed)
        private LocalDateTime postDeadline;
        private Long daysRemaining; // Calculated field
    }
}