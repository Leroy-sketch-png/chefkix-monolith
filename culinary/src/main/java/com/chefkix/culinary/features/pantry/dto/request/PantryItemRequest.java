package com.chefkix.culinary.features.pantry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
    String ingredientName;

    @Positive(message = "Quantity must be positive")
    Double quantity;

    String unit;
    String category;
    LocalDate expiryDate;
}
