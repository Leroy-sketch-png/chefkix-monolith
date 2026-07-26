package com.chefkix.culinary.features.mealplan.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlannedDay {
String dayOfWeek;
    PlannedMeal breakfast;
    PlannedMeal lunch;
    PlannedMeal dinner;
}
