package com.chefkix.culinary.features.challenge.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
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

    int targetCount;
    String targetUnit;

    long currentProgress;
    long participantCount;
double progressPercent;

    int rewardXpPerUser;
    String rewardBadgeId;

String startsAt;
String endsAt;
String status;

boolean hasContributed;

    Map<String, Object> criteria;

    List<String> tags;
}
