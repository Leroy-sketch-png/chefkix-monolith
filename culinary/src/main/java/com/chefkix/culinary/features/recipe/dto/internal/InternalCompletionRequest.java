package com.chefkix.culinary.features.recipe.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InternalCompletionRequest {
    String userId;
    int xpAmount;
    List<String> newBadges;
}