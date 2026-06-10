package com.chefkix.culinary.features.cookplan.controller;

import com.chefkix.culinary.features.cookplan.dto.request.CreateCookPlanRequest;
import com.chefkix.culinary.features.cookplan.dto.request.SwapCookPlanDishRequest;
import com.chefkix.culinary.features.cookplan.entity.CookPlan;
import com.chefkix.culinary.features.cookplan.service.CookPlanService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cook-plans")
@RequiredArgsConstructor
public class CookPlanController {

    private final CookPlanService cookPlanService;

    @PostMapping
    public ApiResponse<CookPlan> create(@Valid @RequestBody CreateCookPlanRequest request) {
        return ApiResponse.created(cookPlanService.create(userId(), request));
    }

    @GetMapping("/current")
    public ApiResponse<CookPlan> current(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate planDate) {
        return ApiResponse.ok(cookPlanService.current(userId(), planDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<CookPlan> get(@PathVariable String id) {
        return ApiResponse.ok(cookPlanService.get(userId(), id));
    }

    @PutMapping("/{id}/batches/{batchId}/dishes/{dishRecipeId}")
    public ApiResponse<CookPlan> swap(
            @PathVariable String id,
            @PathVariable String batchId,
            @PathVariable String dishRecipeId,
            @Valid @RequestBody SwapCookPlanDishRequest request) {
        return ApiResponse.ok(
                cookPlanService.swap(userId(), id, batchId, dishRecipeId, request.getRecipeId()));
    }

    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
