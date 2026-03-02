package com.chefkix.culinary.features.recipe.dto.response;

import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeDetailResponse {

    // --- IDENTITY ---
    String id;
    Instant createdAt;
    Instant updatedAt;
    RecipeStatus recipeStatus;

    // --- AUTHOR INFO ---
    AuthorResponse author;

    // --- CONTENT ---
    String title;
    String description;
    List<String> coverImageUrl;
    List<String> videoUrl;

    Difficulty difficulty;
    int prepTimeMinutes;
    int cookTimeMinutes;
    int totalTimeMinutes;
    int servings;
    String cuisineType;
    List<String> dietaryTags;
    Integer caloriesPerServing;

    List<IngredientResponse> fullIngredientList;
    List<StepResponse> steps;

    // --- GAMIFICATION ---
    int xpReward;
    double difficultyMultiplier;
    List<String> rewardBadges;
    List<String> skillTags;

    // --- AI METADATA ---
    XpBreakdownResponse xpBreakdown;
    ValidationMetadataResponse validation;
    EnrichmentMetadataResponse enrichment;

    // --- GLOBAL STATS ---
    long likeCount;
    long saveCount;
    long viewCount;
    long cookCount;
    long masteredByCount;
    Double trendingScore;
    Double averageRating;
    Integer creatorXpEarned;

    // --- [DYNAMIC] USER CONTEXT ---
    @JsonProperty("isLiked")
    Boolean isLiked;
    @JsonProperty("isSaved")
    Boolean isSaved;
    UserInteractionResponse currentUserInteraction;

    // ==========================================
    // INNER CLASSES (Response DTOs)
    // ==========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInteractionResponse {
        int cookCount;
        Double rating;
        String lastCookedAt;
        String masteryLevel;
        int xpEarnedTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XpBreakdownResponse {
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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationMetadataResponse {
        boolean xpValidated;
        double validationConfidence;
        List<String> validationIssues;
        boolean xpAdjusted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrichmentMetadataResponse {
        List<String> equipmentNeeded;
        List<String> techniqueGuides;
        List<String> seasonalTags;
        Map<String, List<String>> ingredientSubstitutions;
        CulturalContextResponse culturalContext;
        String recipeStory;
        String chefNotes;
        boolean aiEnriched;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CulturalContextResponse {
        String region;
        String background;
        String significance;
    }
}