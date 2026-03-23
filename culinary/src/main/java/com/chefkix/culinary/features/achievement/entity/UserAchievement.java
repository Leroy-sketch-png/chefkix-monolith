package com.chefkix.culinary.features.achievement.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Per-user progress towards a specific achievement.
 * One document per (userId, achievementCode) pair.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "user_achievements")
@CompoundIndexes({
        @CompoundIndex(name = "user_achievement_idx", def = "{'userId': 1, 'achievementCode': 1}", unique = true)
})
public class UserAchievement {
    @Id
    String id;

    @Indexed
    String userId;

    @Indexed
    String achievementCode;

    int currentProgress;
    int requiredProgress; // denormalized from Achievement.criteriaThreshold

    @Builder.Default
    boolean unlocked = false;

    Instant unlockedAt;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
