package com.chefkix.culinary.features.recipe.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO used by Recipe Service to send data to Profile Service
@Data
@Builder
public class InternalCompletionRequest {
    String userId;
    int xpAmount;
    List<String> newBadges;
    // Can add recipeId, rating... if Profile Service needs them
}