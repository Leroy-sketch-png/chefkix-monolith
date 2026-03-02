package com.chefkix.culinary.features.recipe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepRequest {
    private int stepNumber;
    private String title;

    @NotBlank(message = "Step description cannot be blank")
    private String description;

    private String action;

    @Valid // Quan trọng: Yêu cầu Spring validation lồng vào list này
    private List<IngredientRequest> ingredients;

    private Integer timerSeconds;
    private String imageUrl;
    private String tips;
}