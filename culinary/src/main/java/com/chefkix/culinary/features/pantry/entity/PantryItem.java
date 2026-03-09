package com.chefkix.culinary.features.pantry.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Pantry item — one ingredient in a user's kitchen.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "pantry_items")
@CompoundIndexes({
        @CompoundIndex(name = "user_ingredient_unique", def = "{'userId': 1, 'normalizedName': 1}", unique = true),
        @CompoundIndex(name = "user_expiry", def = "{'userId': 1, 'expiryDate': 1}")
})
public class PantryItem {
    @Id
    String id;
    String userId;
    String ingredientName;
    String normalizedName;
    Double quantity;
    String unit;
    String category; // dairy, produce, protein, grain, spice, condiment, other

    LocalDate expiryDate;
    LocalDate addedDate;

    @CreatedDate
    Instant createdAt;
    @LastModifiedDate
    Instant updatedAt;
}
