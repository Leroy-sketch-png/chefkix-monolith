package com.chefkix.culinary.features.recipe.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recent cooking sessions on the creator's recipes.
 * Shows who cooked your recipes and when.
 * Spec: vision_and_spec/21-creator-analytics.txt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecentCookResponse {

    List<RecentCookItem> cooks;
    long totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecentCookItem {
        String sessionId;
        String recipeId;
        String recipeTitle;
        List<String> coverImageUrl;
        String cookUserId;
        String cookDisplayName;
        String cookAvatarUrl;
        String cookUsername;
        LocalDateTime completedAt;
        Integer rating;
        Double xpEarned; // XP the cook earned from this session (shown as creator insight)
    }
}
