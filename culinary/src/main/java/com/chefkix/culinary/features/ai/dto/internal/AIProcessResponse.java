package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AIProcessResponse {
    // --- Core Info ---
    private String title;
    private String description;
    private String difficulty; // Enum string

    private int prepTimeMinutes;
    private int cookTimeMinutes;
    private int totalTimeMinutes;
    private int servings;
    private String cuisineType;
    private List<String> dietaryTags;
    private Integer caloriesPerServing;

    // --- Ingredients & Steps ---
    private List<AiIngredientDto> fullIngredientList;
    private List<AiStepDto> steps;

    // --- Gamification (Matches camelCase alias) ---
    private int xpReward;
    private XpBreakdownDto xpBreakdown;
    private double difficultyMultiplier;
    private List<String> badges;
    private List<String> skillTags;

    // --- Enrichment (Matches alias) ---
    private String recipeStory;
    private List<String> equipmentNeeded;

    // Python returns Dict[str, str] (Skill: Explanation)
    private Map<String, String> techniqueGuides;

    private List<String> seasonalTags;
    private String regionalOrigin;
    private Map<String, List<String>> ingredientSubstitutions;
    private String chefNotes;

    // ================= INNER DTOs =================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AiIngredientDto {
        private String name;
        private String quantity;
        private String unit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AiStepDto {
        private int stepNumber;
        private String title;
        private String description;
        private String action;

        @JsonAlias("timer")
        private Integer timerSeconds;
        private String imageUrl;

        // --- 2 Important fields that were null ---
        private String tips;
        private List<AiIngredientDto> ingredients; // Step-scoped ingredients

        // --- Enriched Fields ---
        private String chefTip;
        private String techniqueExplanation;
        private String commonMistake;
        private Integer estimatedHandsOnTime;
        private List<String> equipmentNeeded;
        private String visualCues;
        private String goal;
        private List<String> microSteps;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class XpBreakdownDto {
        private int base;
        private String baseReason;
        private int steps;
        private String stepsReason;
        private int time;
        private String timeReason;
        private Integer techniques;
        private String techniquesReason;
        private int total;
    }
}