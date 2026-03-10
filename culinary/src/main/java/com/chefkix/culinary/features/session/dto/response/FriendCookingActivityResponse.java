package com.chefkix.culinary.features.session.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for GET /cooking-sessions/friends-active.
 * Shows which of the user's following are currently cooking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendCookingActivityResponse {
    List<ActiveFriend> friends;
    int totalActive;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ActiveFriend {
        String userId;
        String username;
        String displayName;
        String avatarUrl;
        String recipeId;
        String recipeTitle;
        List<String> coverImageUrl;
        int currentStep;
        int totalSteps;
        LocalDateTime startedAt;
        String roomCode; // null for solo, present for co-cooking invite
    }
}
