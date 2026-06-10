package com.chefkix.identity.api.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningPreferences {

    @Builder.Default
    private List<String> dietaryRestrictions = new ArrayList<>();

    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    @Builder.Default
    private List<String> dislikedIngredients = new ArrayList<>();

    @Builder.Default
    private List<String> preferredCuisines = new ArrayList<>();

    private Integer maxCookingTimeMinutes;

    @Builder.Default
    private Integer defaultServings = 2;
}
