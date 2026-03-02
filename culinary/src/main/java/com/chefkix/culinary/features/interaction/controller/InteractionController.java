package com.chefkix.culinary.features.interaction.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.interaction.dto.response.RecipeLikeResponse;
import com.chefkix.culinary.features.interaction.dto.response.RecipeSaveResponse;
import com.chefkix.culinary.features.interaction.service.InteractionService;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // --- ACTIONS ---

    // POST /api/v1/recipes/{id}/like → gateway strips to /{id}/like
    @PostMapping("/{id}/like")
    public ApiResponse<RecipeLikeResponse> toggleLike(@PathVariable String id) {
        return ApiResponse.success(interactionService.toggleLike(id));
    }

    // POST /api/v1/recipes/{id}/save → gateway strips to /{id}/save
    @PostMapping("/{id}/save")
    public ApiResponse<RecipeSaveResponse> toggleSave(@PathVariable String id) {
        return ApiResponse.success(interactionService.toggleSave(id));
    }

    // --- LISTING ---

    // GET /api/v1/recipes/liked → gateway strips to /liked
    @GetMapping("/liked")
    public ApiResponse<List<RecipeSummaryResponse>> getLikedRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.successPage(interactionService.getLikedRecipes(page, size));
    }

    // GET /api/v1/recipes/saved → gateway strips to /saved
    @GetMapping("/saved")
    public ApiResponse<List<RecipeSummaryResponse>> getSavedRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.successPage(interactionService.getSavedRecipes(page, size));
    }
}