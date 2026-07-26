package com.chefkix.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
@Builder
public class CreatorStatsResponse {
    private long totalRecipesPublished;
    private long totalCooksOfYourRecipes;
    private long xpEarnedAsCreator;
private Double avgRating;
    private TopRecipeDto topRecipe;
    private WeeklyStatsDto thisWeek;
    private List<CreatorBadgeDto> creatorBadges;

    @Data
    @Builder
    public static class TopRecipeDto {
        private String id;
        private String title;
private String coverImageUrl;
private Integer cookTimeMinutes;
private String difficulty;
        private long cookCount;
        private long xpGenerated;
private Double averageRating;
    }

    @Data
    @Builder
    public static class WeeklyStatsDto {
        private long newCooks;
        private long xpEarned;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class CreatorBadgeDto {
        private String name;
        private String icon;
private String recipeTitle;
    }
}

