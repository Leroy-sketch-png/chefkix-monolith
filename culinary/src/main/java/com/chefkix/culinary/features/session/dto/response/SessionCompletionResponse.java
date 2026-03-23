package com.chefkix.culinary.features.session.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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

    // Co-op multiplier — only set when cooking in a room with 2+ cooks
    private Double xpMultiplier;          // 1.2 (duo) or 1.1 (group), null if solo
    private String xpMultiplierReason;    // "CO_OP_DUO" or "CO_OP_GROUP"

    // Achievements unlocked by this cooking session
    private List<String> newAchievements; // achievement codes newly unlocked
}