package com.chefkix.culinary.features.session.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    String id;
    String userId;
    LocalDateTime completedAt;

    // --- GAMIFICATION DATA ---
    Double pendingXp;    // XP the user receives when finishing session (typically 30% of total)

    // --- RECIPE INFO ---
    String recipeId;
    String recipeTitle;
    String recipeAuthorId; // ID of the recipe creator
    Double recipeBaseXp;   // Total base XP of the recipe (for calculating 4% bonus)
}