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
 * Response for GET /challenges/weekly.
 * Weekly challenges require multiple completions within a week.
 * Spec: vision_and_spec/13-challenges.txt
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
    String startsAt;  // Monday 00:00 UTC (ISO)
    String endsAt;    // Next Monday 00:00 UTC (ISO)
    Map<String, Object> criteria;
    List<ChallengeResponse.RecipePreviewDto> matchingRecipes;
}
