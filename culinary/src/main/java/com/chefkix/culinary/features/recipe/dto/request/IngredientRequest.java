package com.chefkix.culinary.features.recipe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {
    @NotBlank(message = "Ingredient name cannot be blank")
    @Size(max = 200, message = "Ingredient name must be at most 200 characters")
    private String name;

    @Size(max = 50, message = "Quantity must be at most 50 characters")
    private String quantity;

    @Size(max = 50, message = "Unit must be at most 50 characters")
    private String unit;
}