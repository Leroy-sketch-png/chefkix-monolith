package com.chefkix.culinary.features.achievement.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Achievement blueprint — static definition seeded on startup.
 * Defines what a user must do to unlock an achievement.
 * {@code pathId} groups achievements into progression paths (e.g., "japanese", "saute").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "achievements")
public class Achievement {
    @Id
    String id;

    @Indexed(unique = true)
    String code;

    String name;
    String description;

    @Indexed
    AchievementCategory category;

    int tier; // 1-4

    String icon; // emoji

    @Indexed
    String pathId; // groups into paths: "cuisine_japanese", "technique_saute"

    String prerequisiteCode; // previous achievement in path (null for first)

    CriteriaType criteriaType;
    String criteriaTarget; // "Japanese", "sauté", etc. null for generic criteria
    int criteriaThreshold;

    @Builder.Default
    boolean hidden = false;

    @Builder.Default
    boolean premium = false;

    @CreatedDate
    Instant createdAt;
}
