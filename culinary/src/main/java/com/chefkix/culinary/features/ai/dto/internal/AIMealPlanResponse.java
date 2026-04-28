package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response DTO matching the Python AI service /api/v1/generate_meal_plan output.
 * Deserialized from the "data" field of AIServiceResponse envelope.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AIMealPlanResponse {

    List<AIDayPlan> mealPlan;
    List<String> shoppingList;
    String reasoning;
    double pantryUtilizationPercent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AIDayPlan {
        int day;
        AIMealSlot breakfast;
        AIMealSlot lunch;
        AIMealSlot dinner;
        AIMealSlot snack;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AIMealSlot {
        String name;
        String description;
        int prepTimeMinutes;
        List<String> usesPantry;
        String difficulty;
    }
}
