package com.chefkix.culinary.features.ai.dto.internal;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AIProcessResponse {
    // --- Core Info ---
    private String title;
    private String description;
    private String difficulty; // Enum string

    // Mapping khớp với serialization_alias="prepTimeMinutes"
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

    // --- Gamification (Khớp alias camelCase) ---
    private int xpReward;
    private double difficultyMultiplier;
    private List<String> badges;
    private List<String> skillTags;

    // --- Enrichment (Khớp alias) ---
    private String recipeStory;
    private List<String> equipmentNeeded;

    // Python trả về Dict[str, str] (Skill: Explanation)
    private Map<String, String> techniqueGuides;

    private List<String> seasonalTags;
    private String regionalOrigin;
    private Map<String, List<String>> ingredientSubstitutions;
    private String chefNotes;

    // ================= INNER DTOs =================
    @Data
    public static class AiIngredientDto {
        private String name;
        private String quantity;
        private String unit;
    }

    @Data
    public static class AiStepDto {
        private int stepNumber;
        private String title;
        private String description;
        private String action;
        private Integer timerSeconds;
        private String imageUrl;

        // --- 2 Field quan trọng bạn bị null ---
        private String tips;
        private List<AiIngredientDto> ingredients; // Step-scoped ingredients

        // --- Enriched Fields ---
        private String chefTip;
        private String techniqueExplanation;
        private String commonMistake;
        private Integer estimatedHandsOnTime;
        private List<String> equipmentNeeded;
        private String visualCues;
    }
}