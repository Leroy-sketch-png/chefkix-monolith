package com.chefkix.culinary.features.ai.dto.internal;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AIMetaResponse {

    // ===== 1. CORE GAMIFICATION =====
    private int xpReward; // Auto mapped from JSON "xpReward"

    private XpBreakdownDto xpBreakdown; // Nested Object

    private double difficultyMultiplier;
    private List<String> badges;
    private List<String> skillTags;
    private List<String> achievements;

    // ===== 2. ENRICHMENT =====
    private List<String> equipmentNeeded;
    private List<String> techniqueGuides;
    private List<String> seasonalTags;

    // Map: "Ingredient" -> ["Sub1", "Sub2"]
    private Map<String, List<String>> ingredientSubstitutions;

    // Map: Cultural context
    private CulturalContextDto culturalContext;

    // ===== 3. AI CREATIVE (Story/Tips) =====
    private String recipeStory;
    private String chefNotes;
    private boolean aiEnriched;

    // ===== 4. ANTI-CHEAT VALIDATION =====
    private boolean xpValidated;
    private double validationConfidence;
    private List<String> validationIssues;
    private boolean xpAdjusted;
    private boolean aiUsed;

    // ================= INNER DTOs =================

    @Data
    @NoArgsConstructor
    public static class XpBreakdownDto {
        private int base;
        private String baseReason;
        private int steps;
        private String stepsReason;
        private int time;
        private String timeReason;
        private Integer techniques;
        private String techniquesReason;
        private int total;
    }

    @Data
    @NoArgsConstructor
    public static class CulturalContextDto {
        private String region;
        private String background;
        private String significance;
    }
}