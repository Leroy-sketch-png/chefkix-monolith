package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.identity.api.dto.CompletionResult;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletionResponse {

    String completionId;
    String recipeId;

    int xpEarned;          // Actual XP received (calculated as 50% or 100%)
    List<String> newBadges; // List of newly earned badges (if any)
    CompletionResult userProfile; // Updated profile info for UI update

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @FieldDefaults(level = AccessLevel.PRIVATE)
//    public static class UserProfileSummary {
//        String userId;
//
//        Integer currentLevel;
//        Integer currentXP;
//        Integer currentXPGoal;
//
//        int completionCount;
//    }
}