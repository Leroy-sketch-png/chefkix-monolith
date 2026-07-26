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

    private String id;
    private String title;
    private String description;
    private String icon;
    private int bonusXp;

    private Map<String, Object> criteria;

    private String endsAt;

    private boolean completed;

    private String completedAt;

    private List<RecipePreviewDto> matchingRecipes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipePreviewDto {
        private String id;
        private String title;
private int xpReward;
        private List<String> coverImageUrl;
private int totalTime;
        private Difficulty difficulty;
    }
}