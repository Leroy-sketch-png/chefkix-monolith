package com.chefkix.culinary.features.cookplan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwapCookPlanDishRequest {

    @NotBlank
    private String recipeId;
}
