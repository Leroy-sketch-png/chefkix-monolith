package com.chefkix.culinary.features.recipe.controller;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.features.recipe.dto.response.CreatorPerformanceResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecentCookResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSocialProofResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.service.RecipeService;
import jakarta.validation.Valid;
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

    // 1. UPDATE
    @PutMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody RecipeRequest request) {
        return ApiResponse.success(recipeService.updateRecipe(id, request), "Recipe updated successfully");
    }

    // 3. GET DETAIL
    @GetMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> getById(@PathVariable String id) {
        return ApiResponse.success(recipeService.getRecipeById(id));
    }

    // 3b. SOCIAL PROOF — community validation for a recipe
    @GetMapping("/{id}/social-proof")
    public ApiResponse<RecipeSocialProofResponse> getSocialProof(@PathVariable String id) {
        return ApiResponse.success(recipeService.getRecipeSocialProof(id));
    }

    // 4. SEARCH & FILTER
    // GET /recipes — main search endpoint (accepts query params from RecipeSearchQuery)
    @GetMapping
    public ApiResponse<List<RecipeDetailResponse>> search(
            @ModelAttribute RecipeSearchQuery query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<RecipeDetailResponse> pageResult = recipeService.searchRecipes(query, pageable);
        return ApiResponse.successPage(pageResult);
    }

    // GET /recipes/search — alias for FE convenience (same logic, cleaner semantic URL)
    @GetMapping("/search")
    public ApiResponse<List<RecipeDetailResponse>> searchAlias(
            @ModelAttribute RecipeSearchQuery query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return search(query, pageable);
    }

    // 5. TRENDING
    @GetMapping("/trending")
    public ApiResponse<List<RecipeDetailResponse>> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.successPage(recipeService.getTrendingRecipes(page, size));
    }

    // 6. FRIENDS FEED
    @GetMapping("/feed")
    public ApiResponse<List<RecipeSummaryResponse>> getFriendsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.successPage(recipeService.getFriendsFeed(page, size));
    }

    // 7. GET RECIPES BY USER
    @GetMapping("/user/{userId}")
    public ApiResponse<List<RecipeSummaryResponse>> getRecipesByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.successPage(recipeService.getRecipesByUser(userId, page, size));
    }

    // ===============================================
    // CREATOR ANALYTICS (spec: 21-creator-analytics.txt)
    // ===============================================

    // 8. CREATOR PERFORMANCE — per-recipe metrics
    @GetMapping("/creator/performance")
    public ApiResponse<CreatorPerformanceResponse> getCreatorPerformance() {
        return ApiResponse.success(recipeService.getCreatorPerformance(), "Creator performance retrieved");
    }

    // 9. CREATOR RECENT COOKS — who cooked your recipes
    @GetMapping("/creator/recent-cooks")
    public ApiResponse<RecentCookResponse> getRecentCooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(recipeService.getRecentCooksOfMyRecipes(page, size), "Recent cooks retrieved");
    }
}