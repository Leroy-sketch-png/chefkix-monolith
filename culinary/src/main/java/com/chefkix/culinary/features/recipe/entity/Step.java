package com.chefkix.culinary.features.recipe.entity;

import lombok.Data;
import java.util.List;

@Data
public class Step {
    private int stepNumber;
    private String title;
    private String description;
    private String action;
    private Integer timerSeconds;
    private String imageUrl;

    private String videoUrl;
    private String videoThumbnailUrl;
    private Integer videoDurationSec;

private String tips;
private List<Ingredient> ingredients;

private String chefTip;
    private String techniqueExplanation;
    private String commonMistake;
    private Integer estimatedHandsOnTime;
    private List<String> equipmentNeeded;
    private String visualCues;

private String goal;
private List<String> microSteps;
}