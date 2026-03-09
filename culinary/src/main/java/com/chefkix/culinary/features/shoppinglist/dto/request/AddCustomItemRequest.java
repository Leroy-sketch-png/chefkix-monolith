package com.chefkix.culinary.features.shoppinglist.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddCustomItemRequest {
    @NotBlank String ingredient;
    String quantity;
    String unit;
    String category;
}
