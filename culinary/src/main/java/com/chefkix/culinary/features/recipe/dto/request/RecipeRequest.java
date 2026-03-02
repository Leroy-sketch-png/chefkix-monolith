package com.chefkix.culinary.features.recipe.dto.request;

import com.chefkix.culinary.common.enums.Difficulty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeRequest {

    // --- BASIC INFO ---
    @NotBlank(message = "Title is required")
    String title;

    String description;

    List<String> coverImageUrl;
    List<String> videoUrl;

    // --- METADATA ---
    @NotNull(message = "Difficulty is required")
    Difficulty difficulty;

    int prepTimeMinutes;
    int cookTimeMinutes;
    int totalTimeMinutes;
    int servings;
    String cuisineType;
    List<String> dietaryTags;
    Integer caloriesPerServing;

    // --- STRUCTURE (Nested DTOs) ---
    @Valid
    List<IngredientRequest> fullIngredientList;

    @Valid
    List<StepRequest> steps;

    // --- GAMIFICATION (Config) ---
    int xpReward;
    double difficultyMultiplier;
    List<String> rewardBadges;
    List<String> skillTags;
    Boolean isPublished;

    // --- AI METADATA (Dữ liệu từ AI Service trả về để lưu lại) ---
    XpBreakdownDto xpBreakdown;
    ValidationMetadataDto validation;
    EnrichmentMetadataDto enrichment;


    // ==========================================
    // INNER DTO CLASSES
    // ==========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class XpBreakdownDto {
        int base;
        String baseReason;
        int steps;
        String stepsReason;
        int time;
        String timeReason;
        Integer techniques;
        String techniquesReason;
        int total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationMetadataDto {
        boolean xpValidated;
        double validationConfidence;
        List<String> validationIssues;
        boolean xpAdjusted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrichmentMetadataDto {
        List<String> equipmentNeeded;
        List<String> techniqueGuides;
        List<String> seasonalTags;
        Map<String, List<String>> ingredientSubstitutions;
        CulturalContextDto culturalContext;
        String recipeStory;
        String chefNotes;
        boolean aiEnriched;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CulturalContextDto {
        String region;
        String background;
        String significance;
    }
}