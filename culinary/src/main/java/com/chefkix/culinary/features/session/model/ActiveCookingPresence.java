package com.chefkix.culinary.features.session.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Redis-cached presence data for "Friends Cooking Now" feature.
 * Stored as JSON in Redis with key pattern: cooking:active:{userId}
 * TTL: 4 hours (safety net for abandoned sessions).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveCookingPresence {
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
    String roomCode; // null for solo cooking, present for co-cooking rooms
}
