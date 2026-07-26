package com.chefkix.culinary.features.recipe.entity;

import com.chefkix.culinary.common.enums.Difficulty;
import com.chefkix.culinary.common.enums.MealRole;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.RecipeVisibility;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "recipes")
@CompoundIndexes({
    @CompoundIndex(name = "published_date_idx", def = "{'isPublished': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "status_trending_idx", def = "{'status': 1, 'trendingScore': -1}")
})
public class Recipe {

    @Id
    String id;

    @Version
    Long version;

@Indexed
    String userId;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;

    Instant publishedAt;
    RecipeVisibility recipeVisibility;

    @Indexed
    @Builder.Default
    RecipeStatus status = RecipeStatus.DRAFT;


    @Builder.Default
    List<String> coverImageUrl = new ArrayList<>();

    @Builder.Default
    List<String> videoUrl = new ArrayList<>();

    @TextIndexed(weight = 10)
    String title;

    @TextIndexed(weight = 5)
    String description;

Difficulty difficulty;

    int prepTimeMinutes;
    int cookTimeMinutes;
    int totalTimeMinutes;
    int servings;
    @Indexed(direction = IndexDirection.DESCENDING)
    @Builder.Default
    Double trendingScore = 0.0;

    @Indexed
    @TextIndexed(weight = 3)
    String cuisineType;

    @Builder.Default
    List<String> dietaryTags = new ArrayList<>();

    MealRole mealRole;

    Integer caloriesPerServing;

    @Builder.Default
    List<Ingredient> fullIngredientList = new ArrayList<>();

    @Builder.Default
    List<Step> steps = new ArrayList<>();

    int xpReward;
    double difficultyMultiplier;

    @Builder.Default
List<String> rewardBadges = new ArrayList<>();

    @Builder.Default
    List<String> skillTags = new ArrayList<>();

    XpBreakdown xpBreakdown;

    ValidationMetadata validation;

    EnrichmentMetadata enrichment;

Integer qualityScore;
com.chefkix.culinary.common.enums.QualityTier qualityTier;

    @Builder.Default
    long likeCount = 0;
    @Builder.Default
    long saveCount = 0;
    @Builder.Default
    long viewCount = 0;

    @Builder.Default
long cookCount = 0;

    @Builder.Default
long masteredByCount = 0;

    @Builder.Default
Double averageRating = 0.0;

    @Builder.Default
Integer creatorXpEarned = 0;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class XpBreakdown {
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
    public static class ValidationMetadata {
        boolean xpValidated;
double validationConfidence;

        @Builder.Default
        List<String> validationIssues = new ArrayList<>();

        boolean xpAdjusted;

        boolean aiUsed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrichmentMetadata {
        @Builder.Default
        List<String> equipmentNeeded = new ArrayList<>();

        @Builder.Default
List<String> techniqueGuides = new ArrayList<>();

        @Builder.Default
        List<String> seasonalTags = new ArrayList<>();

        @Builder.Default
        Map<String, List<String>> ingredientSubstitutions = new HashMap<>();

        String regionalOrigin;

        CulturalContext culturalContext;

        String recipeStory;
        String chefNotes;
        boolean aiEnriched;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CulturalContext {
        String region;
        String background;
        String significance;
    }
}
