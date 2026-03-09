package com.chefkix.culinary.features.mealplan.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Weekly meal plan document.
 * Spec: vision_and_spec/23-pantry-and-meal-planning.txt §7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "meal_plans")
public class MealPlan {

    @Id
    String id;

    @Indexed
    String userId;

    LocalDate weekStartDate;

    @Builder.Default
    List<PlannedDay> days = new ArrayList<>();

    @Builder.Default
    List<ShoppingItem> shoppingList = new ArrayList<>();

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
