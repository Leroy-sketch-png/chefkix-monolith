package com.chefkix.culinary.features.pantry.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkPantryItemRequest {
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    List<PantryItemRequest> items;
}
