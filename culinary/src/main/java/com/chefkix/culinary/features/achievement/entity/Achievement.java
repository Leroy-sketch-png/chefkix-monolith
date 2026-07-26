package com.chefkix.culinary.features.achievement.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
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

int tier;

String icon;

    @Indexed
String pathId;

String prerequisiteCode;

    CriteriaType criteriaType;
String criteriaTarget;
    int criteriaThreshold;

    @Builder.Default
    boolean hidden = false;

    @Builder.Default
    boolean premium = false;

    @CreatedDate
    Instant createdAt;
}
