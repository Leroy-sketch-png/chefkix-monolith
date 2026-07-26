package com.chefkix.culinary.features.recipe.controller;

import com.chefkix.culinary.features.recipe.dto.request.RecipePublishRequest;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipePublishResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.service.DraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    @PostMapping("/draft")
    public ApiResponse<RecipeDetailResponse> createDraft() {
        return ApiResponse.success(draftService.createDraft());
    }

    @PatchMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> autoSaveDraft(
            @PathVariable String id,
@RequestBody RecipeRequest request) {
        return ApiResponse.success(draftService.autoSaveDraft(id, request));
    }

    @GetMapping("/drafts")
    public ApiResponse<List<RecipeSummaryResponse>> getMyDrafts() {
        return ApiResponse.success(draftService.getMyDrafts());
    }

    @DeleteMapping("/draft/{id}")
    public ApiResponse<Void> discardDraft(@PathVariable String id) {
        draftService.discardDraft(id);
        return ApiResponse.success(null, "Draft discarded");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<RecipePublishResponse> publishRecipe(
            @PathVariable String id,
            @Valid @RequestBody RecipePublishRequest request) {
        return ApiResponse.success(draftService.publishRecipe(id, request));
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<RecipeDetailResponse> duplicateDraft(@PathVariable String id) {
        return ApiResponse.success(draftService.duplicateDraft(id));
    }
}