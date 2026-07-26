package com.chefkix.culinary.features.recipe.controller;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.features.recipe.dto.response.CreatorPerformanceResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecentCookResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecommendationResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSocialProofResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.dto.response.StepHeatmapResponse;
import com.chefkix.culinary.features.recipe.service.RecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PutMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody RecipeRequest request) {
        return ApiResponse.success(recipeService.updateRecipe(id, request), "Recipe updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        recipeService.deleteRecipe(id);
        return ApiResponse.success(null, "Recipe deleted successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> getById(@PathVariable String id) {
        return ApiResponse.success(recipeService.getRecipeById(id));
    }

    @GetMapping("/{id}/social-proof")
    public ApiResponse<RecipeSocialProofResponse> getSocialProof(@PathVariable String id) {
        return ApiResponse.success(recipeService.getRecipeSocialProof(id));
    }

    @GetMapping
    public ApiResponse<List<RecipeDetailResponse>> search(
            @ModelAttribute RecipeSearchQuery query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<RecipeDetailResponse> pageResult = recipeService.searchRecipes(query, pageable);
        return ApiResponse.successPage(pageResult);
    }

    @GetMapping("/search")
    public ApiResponse<List<RecipeDetailResponse>> searchAlias(
            @ModelAttribute RecipeSearchQuery query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return search(query, pageable);
    }

    @GetMapping("/trending")
    public ApiResponse<List<RecipeDetailResponse>> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") @Max(100) int size
    ) {
        return ApiResponse.successPage(recipeService.getTrendingRecipes(page, size));
    }

    @GetMapping("/feed")
    public ApiResponse<List<RecipeSummaryResponse>> getFriendsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ApiResponse.successPage(recipeService.getFriendsFeed(page, size));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<RecipeSummaryResponse>> getRecipesByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ApiResponse.successPage(recipeService.getRecipesByUser(userId, page, size));
    }


    @GetMapping("/tonight-pick")
    public ApiResponse<RecommendationResponse> getTonightsPick() {
        return ApiResponse.success(recipeService.getTonightsPick());
    }

    @GetMapping("/{id}/similar")
    public ApiResponse<List<RecipeDetailResponse>> getSimilarRecipes(
            @PathVariable String id,
            @RequestParam(defaultValue = "6") @Max(100) int size) {
        return ApiResponse.successPage(recipeService.getSimilarRecipes(id, size));
    }


    @GetMapping("/creator/performance")
    public ApiResponse<CreatorPerformanceResponse> getCreatorPerformance() {
        return ApiResponse.success(recipeService.getCreatorPerformance(), "Creator performance retrieved");
    }

    @GetMapping("/creator/recent-cooks")
    public ApiResponse<RecentCookResponse> getRecentCooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ApiResponse.success(recipeService.getRecentCooksOfMyRecipes(page, size), "Recent cooks retrieved");
    }

    @GetMapping("/{id}/step-heatmap")
    public ApiResponse<StepHeatmapResponse> getStepHeatmap(@PathVariable String id) {
        return ApiResponse.success(recipeService.getStepHeatmap(id), "Step heatmap retrieved");
    }
}