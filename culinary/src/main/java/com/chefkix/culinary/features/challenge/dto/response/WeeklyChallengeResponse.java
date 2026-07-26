package com.chefkix.culinary.features.challenge.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class WeeklyChallengeResponse {

    String id;
    String title;
    String description;
    int bonusXp;
    int target;
    int progress;
    boolean completed;
    String completedAt;
String startsAt;
String endsAt;
    Map<String, Object> criteria;
    List<ChallengeResponse.RecipePreviewDto> matchingRecipes;
}
