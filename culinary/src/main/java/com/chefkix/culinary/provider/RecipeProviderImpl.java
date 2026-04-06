package com.chefkix.culinary.provider;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.dto.CreatorInsightsInfo;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
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
    private final RecipeRepository recipeRepository;

    @Override
    public CreatorInsightsInfo getCreatorInsights(String userId) {
        InternalCreatorInsightsResponse internal = recipeService.getRecipeWithAboveTenCooks(userId);
        int publishedCount = (int) recipeRepository.countByUserIdAndStatus(userId, RecipeStatus.PUBLISHED);

        List<CreatorInsightsInfo.TopRecipeInfo> highPerforming = internal.getHighPerformingRecipes() != null
                ? internal.getHighPerformingRecipes().stream().map(this::mapTopRecipe).collect(Collectors.toList())
                : List.of();

        return CreatorInsightsInfo.builder()
                .totalRecipesPublished(publishedCount)
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

    @Override
    public RecipeSummaryInfo getRecipeSummary(String recipeId) {
        return recipeRepository.findById(recipeId)
                .map(recipe -> RecipeSummaryInfo.builder()
                        .id(recipe.getId())
                        .title(recipe.getTitle())
                        .coverImageUrl(recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty()
                                ? recipe.getCoverImageUrl().get(0)
                                : null)
                        .authorId(recipe.getUserId())
                        .build())
                .orElse(null);
    }
}
