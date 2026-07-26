package com.chefkix.identity.dto.response.internal;

import com.chefkix.identity.dto.response.CreatorStatsResponse;
import lombok.Data;

import java.util.List;

/**
 */
@Data
public class InternalCreatorInsightsResponse {
    private CreatorStatsResponse.TopRecipeDto topRecipe;
    private List<CreatorStatsResponse.TopRecipeDto> highPerformingRecipes;
private Double avgRating;
}