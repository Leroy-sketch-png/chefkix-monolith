package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIMetaRequest {
    // --- Basic Info ---
    private String title;
    private String description;

    // Enum String: "Beginner", "Intermediate", "Advanced", "Expert"
    private String difficulty;

    @JsonProperty("cuisine_type")
    private String cuisineType;

    @JsonProperty("dietary_tags")
    private List<String> dietaryTags;

    @JsonProperty("prep_time_minutes")
    private int prepTimeMinutes;

    @JsonProperty("cook_time_minutes")
    private int cookTimeMinutes;

    private int servings;

    @JsonProperty("calories_per_serving")
    private Integer caloriesPerServing;

    // --- Ingredients & Steps ---
    // Python cần List<Dict>, Java dùng List<Object> hoặc tạo DTO con
    @JsonProperty("full_ingredient_list")
    private List<MetaIngredientDto> fullIngredientList;

    private List<MetaStepDto> steps;

    // --- Options (Flags) ---
    @JsonProperty("include_enrichment")
    @Builder.Default
    private boolean includeEnrichment = true; // Mặc định bật để lấy Story/Tips

    @JsonProperty("include_substitutions")
    @Builder.Default
    private boolean includeSubstitutions = true;

    @JsonProperty("include_equipment")
    @Builder.Default
    private boolean includeEquipment = true;

    @JsonProperty("include_technique_guides")
    @Builder.Default
    private boolean includeTechniqueGuides = true;

    // --- Inner DTOs cho gọn ---
    @Data
    @Builder
    public static class MetaIngredientDto {
        private String name;
        private String quantity;
        private String unit;
    }

    @Data
    @Builder
    public static class MetaStepDto {
        @JsonProperty("stepNumber")
        private int stepNumber;

        private String description;
        private String action;

        @JsonProperty("timerSeconds")
        private Integer timerSeconds;

        private List<MetaIngredientDto> ingredients;
    }
}