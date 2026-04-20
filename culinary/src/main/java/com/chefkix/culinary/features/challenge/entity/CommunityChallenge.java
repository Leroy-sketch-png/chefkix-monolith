package com.chefkix.culinary.features.challenge.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A community-wide challenge where ALL users contribute to a shared goal.
 * Example: "Community cooks 1000 recipes this week."
 * Progress tracked in Redis for real-time atomic increments; this document stores config + final state.
 * Spec: vision_and_spec/13-challenges.txt
 */
@Document(collection = "community_challenges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommunityChallenge {

    @Id
    String id;

    String title;
    String description;
    String emoji; // e.g., "🌍"

    // Goal
    int targetCount; // e.g., 1000 recipes cooked community-wide
    String targetUnit; // e.g., "recipes cooked", "posts shared"

    // Reward for each participant when the community goal is met
    int rewardXpPerUser;
    String rewardBadgeId; // optional badge on completion

    // Scheduling
    Instant startsAt;
    Instant endsAt;

    // Status: ACTIVE, COMPLETED, EXPIRED
    @Indexed
    @Builder.Default
    String status = "ACTIVE";

    // Criteria to count (what qualifies as a contribution)
    // e.g., { "type": "COOK_ANY" } or { "cuisineType": ["Italian"] }
    Map<String, Object> criteria;

    // Snapshot of final progress (set when completed or expired)
    int finalProgress;
    int finalParticipantCount;

    // Tags for filtering/display
    List<String> tags;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
