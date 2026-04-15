package com.chefkix.culinary.features.challenge.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "challenge_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// Important: Ensure a user cannot claim reward twice in one day (database-level logic)
@CompoundIndex(name = "unique_daily_challenge_user", def = "{'userId': 1, 'challengeDate': 1}", unique = true)
public class ChallengeLog {

    @Id
    String id;

    @Indexed
    String userId;

    // Challenge identifier ID (e.g., "noodle-day", "quick-meal")
    // Taken from your config file/enum
    String challengeId;

    // Snapshot of challenge title at time of completion
    // (So if the config file changes the title later, user history display won't break)
    String challengeTitle;

    // Recipe the user cooked to complete this challenge
    String recipeId;
    String recipeTitle;
    // Logical date key: "YYYY-MM-DD" (e.g., "2025-01-16")
    // Using String instead of Date to avoid hour/minute/second issues when checking unique index
    @Indexed
    String challengeDate;

    // Bonus XP received (saved as fixed value for accurate history)
    int bonusXp;

    // Actual timestamp when the record was created
    @CreatedDate
    Instant completedAt;
}