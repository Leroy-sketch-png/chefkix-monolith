package com.chefkix.culinary.features.pantry.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PantryItemResponse {
    String id;
    String ingredientName;
    String normalizedName;
    Double quantity;
    String unit;
    String category;
    LocalDate expiryDate;
    LocalDate addedDate;
    /** expire state: "fresh" | "expiring_soon" | "expired" */
    String freshness;
}
