package com.chefkix.culinary.features.mealplan.controller;

import com.chefkix.culinary.features.mealplan.dto.request.GenerateMealPlanRequest;
import com.chefkix.culinary.features.mealplan.dto.request.SwapMealRequest;
import com.chefkix.culinary.features.mealplan.dto.response.MealPlanResponse;
import com.chefkix.culinary.features.mealplan.entity.ShoppingItem;
import com.chefkix.culinary.features.mealplan.service.MealPlanService;
import com.chefkix.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Meal plan CRUD + generation.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §7
 */
@RestController
@RequestMapping("/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    @PostMapping("/generate")
    public ApiResponse<MealPlanResponse> generate(@Valid @RequestBody GenerateMealPlanRequest request) {
        return ApiResponse.<MealPlanResponse>builder()
                .success(true).statusCode(200)
                .data(mealPlanService.generate(userId(), request))
                .build();
    }

    @GetMapping("/current")
    public ApiResponse<MealPlanResponse> getCurrent() {
        return ApiResponse.<MealPlanResponse>builder()
                .success(true).statusCode(200)
                .data(mealPlanService.getCurrent(userId()))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MealPlanResponse> getById(@PathVariable String id) {
        return ApiResponse.<MealPlanResponse>builder()
                .success(true).statusCode(200)
                .data(mealPlanService.getById(userId(), id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        mealPlanService.delete(userId(), id);
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200)
                .build();
    }

    @PutMapping("/{id}/meals/{day}/{type}")
    public ApiResponse<MealPlanResponse> swapMeal(
            @PathVariable String id,
            @PathVariable String day,
            @PathVariable String type,
            @Valid @RequestBody SwapMealRequest request) {
        return ApiResponse.<MealPlanResponse>builder()
                .success(true).statusCode(200)
                .data(mealPlanService.swapMeal(userId(), id, day, type, request))
                .build();
    }

    @GetMapping("/{id}/shopping-list")
    public ApiResponse<List<ShoppingItem>> getShoppingList(@PathVariable String id) {
        return ApiResponse.<List<ShoppingItem>>builder()
                .success(true).statusCode(200)
                .data(mealPlanService.getShoppingList(userId(), id))
                .build();
    }

    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
