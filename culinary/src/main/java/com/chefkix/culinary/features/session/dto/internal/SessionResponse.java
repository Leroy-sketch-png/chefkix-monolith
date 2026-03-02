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
    Double pendingXp;    // Số XP user nhận được khi finish session (thường là 30% tổng)

    // --- RECIPE INFO ---
    String recipeId;
    String recipeTitle;
    String recipeAuthorId; // ID của người tạo ra công thức này
    Double recipeBaseXp;   // Tổng XP gốc của công thức (để tính 4% bonus)
}