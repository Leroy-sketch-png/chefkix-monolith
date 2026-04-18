package com.chefkix.culinary.features.challenge.dto.response;

import com.chefkix.culinary.common.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {

    // Basic Challenge info (From Pool)
    private String id;
    private String title;
    private String description;
    private String icon;
    private int bonusXp;

    // Metadata to help Frontend display conditions (e.g., cuisineType: ["Italian"])
    private Map<String, Object> criteria;

    // Expiration time (Usually end of day UTC)
    private String endsAt;

    // User's status for this Challenge
    private boolean completed;

    // Completion timestamp (null if not yet completed)
    private String completedAt;

    // Suggested recipes for user to cook
    private List<RecipePreviewDto> matchingRecipes;

    // ==========================================================
    // Inner DTO: Recipe summary (To avoid returning a heavy full Recipe object)
    // You can extract to a separate file if you want to reuse
    // ==========================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipePreviewDto {
        private String id;
        private String title;
        private int xpReward; // Recipe's base XP
        private List<String> coverImageUrl;
        private int totalTime; // minutes
        private Difficulty difficulty;
    }
}