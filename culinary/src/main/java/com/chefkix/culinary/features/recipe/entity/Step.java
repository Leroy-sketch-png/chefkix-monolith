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

    // --- Fields khớp với AI Spec (Bạn đang thiếu 2 cái này) ---
    private String tips;                  // Mẹo cơ bản (AI trả về field "tips")
    private List<Ingredient> ingredients; // Nguyên liệu riêng cho bước này

    // --- Enriched Fields (Flattened - Để phẳng như ý bạn) ---
    private String chefTip;              // Mẹo chuyên gia
    private String techniqueExplanation;
    private String commonMistake;
    private Integer estimatedHandsOnTime;
    private List<String> equipmentNeeded;
    private String visualCues;
}