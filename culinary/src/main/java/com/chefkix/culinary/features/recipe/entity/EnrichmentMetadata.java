package com.chefkix.culinary.features.recipe.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrichmentMetadata {
    @Builder.Default
    List<String> equipmentNeeded = new ArrayList<>();

    @Builder.Default
    List<String> techniqueGuides = new ArrayList<>();

    @Builder.Default
    List<String> seasonalTags = new ArrayList<>();

    Map<String, List<String>> ingredientSubstitutions; // Ingredient substitutions

    CulturalContext culturalContext; // Inner class

    String recipeStory;
    String chefNotes;
    boolean aiEnriched;
}