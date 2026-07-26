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
String emoji;

String theme;
String heroImageUrl;
String accentColor;

int targetCount;
String targetUnit;

    int rewardXp;
String rewardBadgeId;
String rewardBadgeName;

    Instant startsAt;
    Instant endsAt;

    @Indexed
    @Builder.Default
    String status = "UPCOMING";

    Map<String, Object> criteria;

    List<String> featuredRecipeIds;

    List<String> tags;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
