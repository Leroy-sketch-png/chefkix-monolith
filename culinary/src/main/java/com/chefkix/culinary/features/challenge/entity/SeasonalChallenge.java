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
 * A time-limited seasonal/event challenge (admin-seeded, not from pool).
 * Example: "Summer BBQ Festival — Complete 5 grilled recipes and earn the BBQ Master badge."
 * Spec: vision_and_spec/13-challenges.txt
 */
@Document(collection = "seasonal_challenges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeasonalChallenge {

    @Id
    String id;

    String title;
    String description;
    String emoji; // e.g., "☀️"

    // Themed content
    String theme; // e.g., "summer-bbq", "holiday-baking", "spooky-halloween"
    String heroImageUrl; // banner art
    String accentColor; // hex color for themed UI, e.g., "#FF6B35"

    // Goal per user
    int targetCount; // how many qualifying recipes each user must cook
    String targetUnit; // "recipes cooked", "techniques mastered"

    // Rewards
    int rewardXp;
    String rewardBadgeId; // badge awarded on personal completion
    String rewardBadgeName; // snapshot for display

    // Scheduling
    Instant startsAt;
    Instant endsAt;

    // Status: UPCOMING, ACTIVE, COMPLETED, EXPIRED
    @Indexed
    @Builder.Default
    String status = "UPCOMING";

    // Criteria for qualifying recipes
    // e.g., { "cuisineType": ["Mexican", "Tex-Mex"], "skillTags": ["grilling"] }
    Map<String, Object> criteria;

    // Curated recipe list (admin picks)
    List<String> featuredRecipeIds;

    // Tags for filtering
    List<String> tags;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
