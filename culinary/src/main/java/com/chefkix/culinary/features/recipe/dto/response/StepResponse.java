package com.chefkix.culinary.features.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResponse {
    private int stepNumber;
    private String title;
    private String description;
    private String action;
    private List<IngredientResponse> ingredients;
    private Integer timerSeconds;
    private String imageUrl;
    private String videoUrl;
    private String videoThumbnailUrl;
    private Integer videoDurationSec;
    private String tips;

    // --- Enriched Fields (AI-generated, exposed for CookingPlayer) ---
    private String chefTip;
    private String techniqueExplanation;
    private String commonMistake;
    private Integer estimatedHandsOnTime;
    private List<String> equipmentNeeded;
    private String visualCues;

    // --- Step V2 Fields (Goal-oriented cooking) ---
    private String goal;
    private List<String> microSteps;
}