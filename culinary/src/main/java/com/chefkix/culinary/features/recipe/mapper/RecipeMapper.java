package com.chefkix.culinary.features.recipe.mapper;

import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.report.dto.internal.InternalCreatorInsightsResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        uses = {IngredientMapper.class, StepMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface RecipeMapper {

    // --- EXISTING METHODS (UNCHANGED) ---
    Recipe toRecipe(RecipeRequest request);
    RecipeDetailResponse toRecipeDetailResponse(Recipe recipe);
    RecipeSummaryResponse toRecipeSummaryResponse(Recipe recipe);

    // Update: IGNORE collections here - they are handled explicitly in DraftService
    // MapStruct's collection mapping with IGNORE strategy doesn't replace properly
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "fullIngredientList", ignore = true)
    void updateRecipeFromRequest(@MappingTarget Recipe recipe, RecipeRequest request);

    @Mapping(target = "xpGenerated", source = "creatorXpEarned")
    @Mapping(target = "coverImageUrl", expression = "java(extractFirstImage(recipe))")
    @Mapping(target = "difficulty", expression = "java(mapDifficulty(recipe))")
    InternalCreatorInsightsResponse.TopRecipeDto toRecipeDto(Recipe recipe);

    /**
     * Extract first image from coverImageUrl list (or null if empty)
     */
    default String extractFirstImage(Recipe recipe) {
        if (recipe.getCoverImageUrl() == null || recipe.getCoverImageUrl().isEmpty()) {
            return null;
        }
        return recipe.getCoverImageUrl().get(0);
    }

    /**
     * Map difficulty enum to @JsonValue format (Title Case)
     */
    default String mapDifficulty(Recipe recipe) {
        if (recipe.getDifficulty() == null) {
            return null;
        }
        // Difficulty enum uses @JsonValue for Title Case (e.g., "Beginner")
        return recipe.getDifficulty().getValue();
    }
}