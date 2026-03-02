package com.chefkix.culinary.provider;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.dto.CreatorInsightsInfo;
import com.chefkix.culinary.features.recipe.service.RecipeService;
import com.chefkix.culinary.features.report.dto.internal.InternalCreatorInsightsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provider implementation exposing culinary recipe data to other modules.
 * Delegates to internal RecipeService.
 */
@Component
@RequiredArgsConstructor
public class RecipeProviderImpl implements RecipeProvider {

    private final RecipeService recipeService;

    @Override
    public CreatorInsightsInfo getCreatorInsights(String userId) {
        InternalCreatorInsightsResponse internal = recipeService.getRecipeWithAboveTenCooks(userId);

        List<CreatorInsightsInfo.TopRecipeInfo> highPerforming = internal.getHighPerformingRecipes() != null
                ? internal.getHighPerformingRecipes().stream().map(this::mapTopRecipe).collect(Collectors.toList())
                : List.of();

        return CreatorInsightsInfo.builder()
                .totalRecipesPublished(highPerforming.size()) // Approximation from available data
                .avgRating(internal.getAvgRating() != null ? internal.getAvgRating() : 0.0)
                .topRecipes(internal.getTopRecipe() != null ? List.of(mapTopRecipe(internal.getTopRecipe())) : List.of())
                .highPerformingRecipes(highPerforming)
                .build();
    }

    private CreatorInsightsInfo.TopRecipeInfo mapTopRecipe(InternalCreatorInsightsResponse.TopRecipeDto dto) {
        return CreatorInsightsInfo.TopRecipeInfo.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .cookCount(dto.getCookCount())
                .xpGenerated(dto.getXpGenerated())
                .coverImageUrl(dto.getCoverImageUrl())
                .cookTimeMinutes(dto.getCookTimeMinutes())
                .difficulty(dto.getDifficulty())
                .averageRating(dto.getAverageRating())
                .build();
    }
}
