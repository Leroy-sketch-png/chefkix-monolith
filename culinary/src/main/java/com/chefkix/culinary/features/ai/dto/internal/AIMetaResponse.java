package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIMetaResponse {

    @JsonAlias("xp_reward")
private int xpReward;

    @JsonAlias("xp_breakdown")
private XpBreakdownDto xpBreakdown;

    @JsonAlias("difficulty_multiplier")
    private double difficultyMultiplier;
    private List<String> badges;
    @JsonAlias("skill_tags")
    private List<String> skillTags;
    private List<String> achievements;

    @JsonAlias("equipment_needed")
    private List<String> equipmentNeeded;
    @JsonAlias("technique_guides")
    private List<String> techniqueGuides;
    @JsonAlias("seasonal_tags")
    private List<String> seasonalTags;

    @JsonAlias("ingredient_substitutions")
    private Map<String, List<String>> ingredientSubstitutions;

    @JsonAlias("cultural_context")
    private CulturalContextDto culturalContext;

    @JsonAlias("recipe_story")
    private String recipeStory;
    @JsonAlias("chef_notes")
    private String chefNotes;
    @JsonAlias("ai_enriched")
    private boolean aiEnriched;

    @JsonAlias("xp_validated")
    private boolean xpValidated;
    @JsonAlias("validation_confidence")
    private double validationConfidence;
    @JsonAlias("validation_issues")
    private List<String> validationIssues;
    @JsonAlias("xp_adjusted")
    private boolean xpAdjusted;
    @JsonAlias("ai_used")
    private boolean aiUsed;


    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class XpBreakdownDto {
        private int base;
        @JsonAlias("base_reason")
        private String baseReason;
        private int steps;
        @JsonAlias("steps_reason")
        private String stepsReason;
        private int time;
        @JsonAlias("time_reason")
        private String timeReason;
        private Integer techniques;
        @JsonAlias("techniques_reason")
        private String techniquesReason;
        private int total;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CulturalContextDto {
        private String region;
        @JsonAlias("tradition")
        private String background;
        private String significance;
    }
}