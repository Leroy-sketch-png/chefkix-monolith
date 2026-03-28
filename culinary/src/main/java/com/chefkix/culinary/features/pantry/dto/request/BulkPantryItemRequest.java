package com.chefkix.culinary.features.pantry.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(max = 50, message = "Maximum 50 items per bulk add")
    @Valid
    List<PantryItemRequest> items;
}
