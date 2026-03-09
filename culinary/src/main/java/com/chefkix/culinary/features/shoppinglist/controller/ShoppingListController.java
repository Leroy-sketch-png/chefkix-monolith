package com.chefkix.culinary.features.shoppinglist.controller;

import com.chefkix.culinary.features.shoppinglist.dto.request.AddCustomItemRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateCustomListRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromMealPlanRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromRecipeRequest;
import com.chefkix.culinary.features.shoppinglist.dto.response.ShoppingListResponse;
import com.chefkix.culinary.features.shoppinglist.dto.response.ShoppingListSummaryResponse;
import com.chefkix.culinary.features.shoppinglist.service.ShoppingListService;
import com.chefkix.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    // ── Create ──────────────────────────────────────────────────────

    @PostMapping("/from-meal-plan")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShoppingListResponse> createFromMealPlan(
            @Valid @RequestBody CreateFromMealPlanRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(201)
                .data(shoppingListService.createFromMealPlan(userId(), request))
                .build();
    }

    @PostMapping("/from-recipe")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShoppingListResponse> createFromRecipe(
            @Valid @RequestBody CreateFromRecipeRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(201)
                .data(shoppingListService.createFromRecipe(userId(), request))
                .build();
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShoppingListResponse> createCustom(
            @Valid @RequestBody CreateCustomListRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(201)
                .data(shoppingListService.createCustom(userId(), request))
                .build();
    }

    // ── Read ────────────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<List<ShoppingListSummaryResponse>> getUserLists() {
        return ApiResponse.<List<ShoppingListSummaryResponse>>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.getUserLists(userId()))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ShoppingListResponse> getById(@PathVariable String id) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.getById(userId(), id))
                .build();
    }

    @GetMapping("/shared/{shareToken}")
    public ApiResponse<ShoppingListResponse> getByShareToken(@PathVariable String shareToken) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.getByShareToken(shareToken))
                .build();
    }

    // ── Item Operations ─────────────────────────────────────────────

    @PutMapping("/{id}/items/{itemId}/toggle")
    public ApiResponse<ShoppingListResponse> toggleItem(
            @PathVariable String id, @PathVariable String itemId) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.toggleItem(userId(), id, itemId))
                .build();
    }

    @PostMapping("/{id}/items")
    public ApiResponse<ShoppingListResponse> addCustomItem(
            @PathVariable String id, @Valid @RequestBody AddCustomItemRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.addCustomItem(userId(), id, request))
                .build();
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<ShoppingListResponse> removeItem(
            @PathVariable String id, @PathVariable String itemId) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.removeItem(userId(), id, itemId))
                .build();
    }

    // ── Delete ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        shoppingListService.delete(userId(), id);
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200)
                .build();
    }

    // ── Share ───────────────────────────────────────────────────────

    @PostMapping("/{id}/share")
    public ApiResponse<ShoppingListResponse> regenerateShareToken(@PathVariable String id) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.regenerateShareToken(userId(), id))
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
