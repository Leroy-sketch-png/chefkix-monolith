package com.chefkix.identity.dto.response.internal;

import com.chefkix.identity.dto.response.CreatorStatsResponse;
import lombok.Data;

import java.util.List;

/**
 * Internal DTO for receiving creator insights from recipe-service via Feign.
 * Must match recipe-service's InternalCreatorInsightsResponse structure.
 */
@Data
public class InternalCreatorInsightsResponse {
    private CreatorStatsResponse.TopRecipeDto topRecipe;
    private List<CreatorStatsResponse.TopRecipeDto> highPerformingRecipes;
    private Double avgRating; // Average rating across all creator's recipes
}