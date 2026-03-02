package com.chefkix.culinary.features.recipe.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.report.dto.internal.InternalCreatorInsightsResponse;
import com.chefkix.culinary.features.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/recipes")
@RequiredArgsConstructor
public class InternalRecipeController {

    private final RecipeService recipeService;

    @GetMapping("/creator-insights/{userId}")
    public ApiResponse<InternalCreatorInsightsResponse> getCreatorInsights(@PathVariable String userId) {
        InternalCreatorInsightsResponse response = recipeService.getRecipeWithAboveTenCooks(userId);
        return ApiResponse.success(response);
    }
}