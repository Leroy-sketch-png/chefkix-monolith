package com.chefkix.culinary.features.session.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CookCardDataResponse {
    String sessionId;
    LocalDateTime completedAt;
    Integer xpEarned;
    Integer stepsCompleted;
    Integer totalSteps;
    Long cookingTimeMinutes;
    Integer rating;

    String recipeId;
    String recipeTitle;
    List<String> coverImageUrl;
    String difficulty;

    String userId;
    String displayName;
    String avatarUrl;

    String shareUrl;
}
