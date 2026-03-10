package com.chefkix.culinary.features.ai.controller.internal;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.ai.dto.internal.AIProcessRequest;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.ai.service.AiIntegrationService;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AiIntegrationService aiIntegrationService;
    private final RecipeRepository recipeRepository;

    @PostMapping("/generate")
    public ApiResponse<RecipeDetailResponse> generateRecipe(@RequestBody AIProcessRequest request) {
        // SECURITY: Use JWT userId, never trust request body userId
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.getRawText() == null) {
            throw new RuntimeException("rawText must not be null");
        }
        RecipeDetailResponse response = aiIntegrationService.createRecipeFromText(request.getRawText(), userId);
        return ApiResponse.success(response);
    }

    /**
     * Endpoint: Recalculate metas for a recipe (Manual / Edited)
     * URL: POST /ai/analyze/{recipeId}
     * SECURITY: Only recipe owner can trigger analysis.
     */
    @PostMapping("/analyze/{recipeId}")
    public ApiResponse<RecipeDetailResponse> analyzeRecipe(@PathVariable String recipeId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        if (!userId.equals(recipe.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        RecipeDetailResponse response = aiIntegrationService.calculateAndEnrichRecipe(recipeId);
        return ApiResponse.success(response);
    }
}
