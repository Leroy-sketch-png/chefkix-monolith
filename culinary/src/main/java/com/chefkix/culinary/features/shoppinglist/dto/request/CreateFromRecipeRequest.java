package com.chefkix.culinary.features.shoppinglist.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateFromRecipeRequest {
    @NotBlank @Size(max = 100) String recipeId;
    @Min(1) @Max(20) int servings = 1;
}
