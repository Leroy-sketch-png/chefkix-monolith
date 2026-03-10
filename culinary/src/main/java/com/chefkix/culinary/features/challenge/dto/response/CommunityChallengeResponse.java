package com.chefkix.culinary.features.challenge.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
 * Response for GET /challenges/community.
 * Shows active community challenges with live progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommunityChallengeResponse {

    String id;
    String title;
    String description;
    String emoji;

    // Goal
    int targetCount;
    String targetUnit;

    // Live progress (from Redis)
    long currentProgress;
    long participantCount;
    double progressPercent; // 0.0 - 100.0

    // Reward
    int rewardXpPerUser;
    String rewardBadgeId;

    // Scheduling
    String startsAt; // ISO8601
    String endsAt;   // ISO8601
    String status;   // ACTIVE, COMPLETED, EXPIRED

    // User-specific
    boolean hasContributed; // whether current user is in participants set

    // Criteria for display
    Map<String, Object> criteria;

    // Tags
    List<String> tags;
}
