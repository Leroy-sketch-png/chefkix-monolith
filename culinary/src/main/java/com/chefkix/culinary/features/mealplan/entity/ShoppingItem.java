package com.chefkix.culinary.features.mealplan.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Single item in the auto-generated shopping list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingItem {
    String ingredient;
    String quantity;
    @Builder.Default
    java.util.List<String> recipes = new java.util.ArrayList<>();
}
