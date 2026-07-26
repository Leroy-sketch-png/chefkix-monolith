package com.chefkix.culinary.features.challenge.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeasonalChallengeResponse {

    String id;
    String title;
    String description;
    String emoji;

    String theme;
    String heroImageUrl;
    String accentColor;

    int targetCount;
    String targetUnit;

    int rewardXp;
    String rewardBadgeId;
    String rewardBadgeName;

String startsAt;
String endsAt;
String status;

int userProgress;
boolean userCompleted;
String userCompletedAt;

    Map<String, Object> criteria;

    List<ChallengeResponse.RecipePreviewDto> featuredRecipes;

    List<String> tags;
}
