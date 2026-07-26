package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIMetaRequest {
    private String title;
    private String description;

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

    @JsonProperty("full_ingredient_list")
    private List<MetaIngredientDto> fullIngredientList;

    private List<MetaStepDto> steps;

    @JsonProperty("include_enrichment")
    @Builder.Default
private boolean includeEnrichment = true;

    @JsonProperty("include_substitutions")
    @Builder.Default
    private boolean includeSubstitutions = true;

    @JsonProperty("include_equipment")
    @Builder.Default
    private boolean includeEquipment = true;

    @JsonProperty("include_technique_guides")
    @Builder.Default
    private boolean includeTechniqueGuides = true;

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