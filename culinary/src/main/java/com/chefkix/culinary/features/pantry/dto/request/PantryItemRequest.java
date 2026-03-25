package com.chefkix.culinary.features.pantry.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PantryItemRequest {
    @NotBlank(message = "Ingredient name is required")
    @Size(max = 200, message = "Ingredient name must be at most 200 characters")
    String ingredientName;

    @Positive(message = "Quantity must be positive")
    @Max(value = 99999, message = "Quantity must be at most 99999")
    Double quantity;

    @Size(max = 50, message = "Unit must be at most 50 characters")
    String unit;

    @Size(max = 100, message = "Category must be at most 100 characters")
    String category;
    LocalDate expiryDate;
}
