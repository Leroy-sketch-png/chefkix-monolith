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

    // Chỉ gọi API này khi user bấm "CÓ" trong popup
    // 1. INIT DRAFT (Gọi ngay khi user bắt đầu viết)
    @PostMapping("/draft")
    public ApiResponse<RecipeDetailResponse> createDraft() {
        return ApiResponse.success(draftService.createDraft());
    }

    // 2. AUTO-SAVE (Dùng PATCH để update một phần)
    // FE gọi cái này mỗi khi debounce (3-5s) hoặc onBlur
    @PatchMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> autoSaveDraft(
            @PathVariable String id,
            @Valid @RequestBody RecipeRequest request) { // Request này cho phép các trường null
        return ApiResponse.success(draftService.autoSaveDraft(id, request));
    }

    // 3. GET MY DRAFTS (Lấy danh sách nháp của tôi)
    @GetMapping("/drafts")
    public ApiResponse<List<RecipeSummaryResponse>> getMyDrafts() {
        return ApiResponse.success(draftService.getMyDrafts());
    }

    // 4. DISCARD DRAFT (Xóa nháp)
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