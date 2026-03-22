package com.chefkix.culinary.features.mealplan.dto.response;

import com.chefkix.culinary.features.mealplan.entity.PlannedDay;
import com.chefkix.culinary.features.mealplan.entity.ShoppingItem;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * Full meal plan response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MealPlanResponse {
    String id;
    LocalDate weekStartDate;
    List<PlannedDay> days;
    List<ShoppingItem> shoppingList;
    String reasoning;
    Double pantryUtilizationPercent;
}
