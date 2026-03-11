package com.chefkix.culinary.features.pantry.controller;

import com.chefkix.culinary.features.pantry.dto.request.BulkPantryItemRequest;
import com.chefkix.culinary.features.pantry.dto.request.PantryItemRequest;
import com.chefkix.culinary.features.pantry.dto.response.PantryItemResponse;
import com.chefkix.culinary.features.pantry.dto.response.PantryRecipeMatchResponse;
import com.chefkix.culinary.features.pantry.service.PantryService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pantry CRUD + recipe matching.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §1-§3
 */
@RestController
@RequestMapping("/pantry")
@RequiredArgsConstructor
public class PantryController {

    private final PantryService pantryService;

    // ── CRUD ────────────────────────────────────────────────────────

    @PostMapping
    public ApiResponse<PantryItemResponse> addItem(@Valid @RequestBody PantryItemRequest request) {
        return ApiResponse.<PantryItemResponse>builder()
                .success(true).statusCode(200)
                .data(pantryService.addItem(userId(), request))
                .build();
    }

    @PostMapping("/bulk")
    public ApiResponse<List<PantryItemResponse>> bulkAdd(@Valid @RequestBody BulkPantryItemRequest request) {
        return ApiResponse.<List<PantryItemResponse>>builder()
                .success(true).statusCode(200)
                .data(pantryService.bulkAdd(userId(), request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PantryItemResponse>> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "category") String sort) {
        return ApiResponse.<List<PantryItemResponse>>builder()
                .success(true).statusCode(200)
                .data(pantryService.getAll(userId(), category, sort))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PantryItemResponse> updateItem(
            @PathVariable String id,
            @Valid @RequestBody PantryItemRequest request) {
        return ApiResponse.<PantryItemResponse>builder()
                .success(true).statusCode(200)
                .data(pantryService.updateItem(userId(), id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable String id) {
        pantryService.deleteItem(userId(), id);
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200)
                .build();
    }

    @DeleteMapping("/expired")
    public ApiResponse<Map<String, Long>> clearExpired() {
        long count = pantryService.clearExpired(userId());
        return ApiResponse.<Map<String, Long>>builder()
                .success(true).statusCode(200)
                .data(Map.of("removed", count))
                .build();
    }

    // ── Recipe Matching ─────────────────────────────────────────────

    @GetMapping("/recipes")
    public ApiResponse<List<PantryRecipeMatchResponse>> matchRecipes(
            @RequestParam(defaultValue = "0.3") double minMatch,
            @RequestParam(defaultValue = "false") boolean prioritizeExpiring) {
        return ApiResponse.<List<PantryRecipeMatchResponse>>builder()
                .success(true).statusCode(200)
                .data(pantryService.findMatchingRecipes(userId(), minMatch, prioritizeExpiring))
                .build();
    }

    // ── Auth ────────────────────────────────────────────────────────

    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
