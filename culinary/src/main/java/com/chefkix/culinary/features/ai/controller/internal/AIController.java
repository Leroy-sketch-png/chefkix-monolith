package com.chefkix.culinary.features.ai.controller.internal;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.culinary.features.ai.dto.internal.AIProcessRequest;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.ai.service.AiIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AiIntegrationService aiIntegrationService;

    @PostMapping("/generate")
    public ApiResponse<RecipeDetailResponse> generateRecipe(@RequestBody AIProcessRequest request) {
        String userId = request.getUserId();
        System.out.println("DEBUG CHECK INPUT: " + request.getRawText());

        if (request.getRawText() == null) {
            throw new RuntimeException("Lỗi: Java nhận được rawText là NULL!");
        }
        RecipeDetailResponse response = aiIntegrationService.createRecipeFromText(request.getRawText(), userId);
        return ApiResponse.success(response);
    }

    // Trong AIController.java

    /**
     * Endpoint: Phân tích và Tính điểm lại cho bài viết (Manual / Edited)
     * URL: POST /ai/analyze/{recipeId}
     */
    @PostMapping("/analyze/{recipeId}")
    public ApiResponse<RecipeDetailResponse> analyzeRecipe(@PathVariable String recipeId) {
        // Gọi hàm service vừa viết
        RecipeDetailResponse response = aiIntegrationService.calculateAndEnrichRecipe(recipeId);
        return ApiResponse.success(response);
    }
}
