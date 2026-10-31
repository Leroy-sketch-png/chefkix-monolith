package com.chefkix.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Premium creator studio analytics response.
 * Gated by {@link com.chefkix.identity.entity.PremiumFeature#ADVANCED_ANALYTICS}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedCreatorStatsResponse {
    private CreatorStatsResponse baseStats;
    private List<CreatorStatsResponse.TopRecipeDto> highPerformingRecipes;
    private Double audienceRetentionRate;
    private Long totalSavesAcrossRecipes;
}

