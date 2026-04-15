package com.chefkix.culinary.features.recipe.entity;

import lombok.Data;
import java.util.List;

@Data
public class Step {
    // --- Core Fields ---
    private int stepNumber;
    private String title;
    private String description;
    private String action;
    private Integer timerSeconds;
    private String imageUrl;

    // --- Video Fields (per spec 20-media-lifecycle.txt) ---
    private String videoUrl;
    private String videoThumbnailUrl;
    private Integer videoDurationSec;

    // --- Fields matching AI Spec (previously missing) ---
    private String tips;                  // Basic tips (AI returns "tips" field)
    private List<Ingredient> ingredients; // Ingredients specific to this step

    // --- Enriched Fields (Flattened) ---
    private String chefTip;              // Expert chef tip
    private String techniqueExplanation;
    private String commonMistake;
    private Integer estimatedHandsOnTime;
    private List<String> equipmentNeeded;
    private String visualCues;

    // --- Step V2 Fields (Goal-oriented cooking) ---
    private String goal;                 // What this step achieves (e.g., "Develop caramelization on the surface")
    private List<String> microSteps;     // 2-5 atomic sub-actions for beginners
}