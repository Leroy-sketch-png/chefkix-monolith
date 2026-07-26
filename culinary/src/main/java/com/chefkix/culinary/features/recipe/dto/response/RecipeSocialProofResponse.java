package com.chefkix.culinary.features.recipe.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeSocialProofResponse {
    long cookCount;
    long postCount;
    Double averageRating;
    List<RecentCooker> recentCookers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecentCooker {
        String userId;
        String username;
        String displayName;
        String avatarUrl;
        LocalDateTime completedAt;
    }
}
