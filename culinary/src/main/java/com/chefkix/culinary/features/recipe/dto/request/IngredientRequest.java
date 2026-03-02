package com.chefkix.culinary.features.recipe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {
    @NotBlank(message = "Ingredient name cannot be blank")
    private String name;
    private String quantity;
    private String unit;
}