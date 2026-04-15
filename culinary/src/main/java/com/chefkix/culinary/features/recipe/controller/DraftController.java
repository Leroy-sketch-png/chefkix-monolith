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

    // Only call this API when user clicks "YES" in the popup
    // 1. INIT DRAFT
    @PostMapping("/draft")
    public ApiResponse<RecipeDetailResponse> createDraft() {
        return ApiResponse.success(draftService.createDraft());
    }

    // 2. AUTO-SAVE (Use PATCH for partial update)
    // FE calls this on debounce (3-5s) or onBlur
    @PatchMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> autoSaveDraft(
            @PathVariable String id,
            @RequestBody RecipeRequest request) { // No @Valid — auto-save accepts partial data; validation at publish
        return ApiResponse.success(draftService.autoSaveDraft(id, request));
    }

    // 3. GET MY DRAFTS
    @GetMapping("/drafts")
    public ApiResponse<List<RecipeSummaryResponse>> getMyDrafts() {
        return ApiResponse.success(draftService.getMyDrafts());
    }

    // 4. DISCARD DRAFT
    @DeleteMapping("/draft/{id}")
    public ApiResponse<Void> discardDraft(@PathVariable String id) {
        draftService.discardDraft(id);
        return ApiResponse.success(null, "Draft discarded");
    }

    // 5. PUBLISH RECIPE
    @PostMapping("/{id}/publish")
    public ApiResponse<RecipePublishResponse> publishRecipe(
            @PathVariable String id,
            @Valid @RequestBody RecipePublishRequest request) {
        return ApiResponse.success(draftService.publishRecipe(id, request));
    }

    // 6. DUPLICATE RECIPE (creates a new DRAFT from any owned recipe)
    @PostMapping("/{id}/duplicate")
    public ApiResponse<RecipeDetailResponse> duplicateDraft(@PathVariable String id) {
        return ApiResponse.success(draftService.duplicateDraft(id));
    }
}