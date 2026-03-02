package com.chefkix.culinary.features.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResponse {
    private int stepNumber;
    private String title;
    private String description;
    private String action;
    private List<IngredientResponse> ingredients;
    private Integer timerSeconds;
    private String imageUrl;
    private String tips;
}