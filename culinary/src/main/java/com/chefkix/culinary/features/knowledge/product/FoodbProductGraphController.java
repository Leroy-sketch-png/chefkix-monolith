package com.chefkix.culinary.features.knowledge.product;

import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge/product")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class FoodbProductGraphController {
    private final FoodbProductGraphService graphService;

    @GetMapping("/foods")
    public ResponseEntity<ApiResponse<FoodbProductGraphService.FoodSearchResponse>> searchFoods(
            @RequestParam("q") @NotBlank @Size(max = 120) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        try {
            var result = graphService.searchFoods(query, limit);
            return ResponseEntity.ok(ApiResponse.<FoodbProductGraphService.FoodSearchResponse>builder()
                    .success(true).statusCode(200).data(result).build());
        } catch (IllegalArgumentException invalid) {
            return ResponseEntity.badRequest().body(ApiResponse.<FoodbProductGraphService.FoodSearchResponse>builder()
                    .success(false).statusCode(400).message("Invalid product graph query").build());
        } catch (FoodbProductGraphService.ProductGraphUnavailableException unavailable) {
            return ResponseEntity.status(503).body(ApiResponse.<FoodbProductGraphService.FoodSearchResponse>builder()
                    .success(false).statusCode(503).message("Product graph unavailable; no other graph was substituted")
                    .build());
        }
    }

    @GetMapping("/foods/{foodId}/compounds")
    public ResponseEntity<ApiResponse<FoodbProductGraphService.FoodCompositionResponse>> getFoodComposition(
            @PathVariable String foodId) {
        try {
            var result = graphService.getFoodComposition(foodId);
            return ResponseEntity.ok(ApiResponse.<FoodbProductGraphService.FoodCompositionResponse>builder()
                    .success(true).statusCode(200).data(result).build());
        } catch (IllegalArgumentException invalid) {
            return ResponseEntity.badRequest().body(ApiResponse.<FoodbProductGraphService.FoodCompositionResponse>builder()
                    .success(false).statusCode(400).message("Invalid source Food ID").build());
        } catch (FoodbProductGraphService.ProductGraphUnavailableException unavailable) {
            return ResponseEntity.status(503).body(ApiResponse.<FoodbProductGraphService.FoodCompositionResponse>builder()
                    .success(false).statusCode(503).message("Product graph unavailable; no other graph was substituted")
                    .build());
        }
    }
}
