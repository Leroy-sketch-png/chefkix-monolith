package com.chefkix.culinary.features.session.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionCompletionResponse {
    private String sessionId;
    private String status;
    private Integer baseXpAwarded; // XP earned immediately (30%) - integer for game systems
    private Integer pendingXp;     // XP pending until post (70%) - integer for game systems
    private String message;
    private LocalDateTime postDeadline;
    
    // Level-up information
    private Boolean leveledUp;    // True if user leveled up from this completion
    private Integer oldLevel;     // Level before completion
    private Integer newLevel;     // Level after completion (if leveledUp)
    private Integer currentXp;    // Total XP after this completion
    private Integer xpToNextLevel; // XP needed for next level
}