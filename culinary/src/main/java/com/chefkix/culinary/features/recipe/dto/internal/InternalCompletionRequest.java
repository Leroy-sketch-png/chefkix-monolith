package com.chefkix.culinary.features.recipe.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO này Recipe Service dùng để gửi sang Profile Service
@Data
@Builder
public class InternalCompletionRequest {
    String userId;
    int xpAmount;
    List<String> newBadges;
    // Có thể thêm recipeId, rating... nếu Profile Service cần
}