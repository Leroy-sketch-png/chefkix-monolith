package com.chefkix.culinary.features.shoppinglist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddCustomItemRequest {
    @NotBlank
    @Size(max = 200, message = "Ingredient must be at most 200 characters")
    String ingredient;

    @Size(max = 50, message = "Quantity must be at most 50 characters")
    String quantity;

    @Size(max = 50, message = "Unit must be at most 50 characters")
    String unit;

    @Size(max = 100, message = "Category must be at most 100 characters")
    String category;
}
