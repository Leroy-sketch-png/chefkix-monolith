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
@CompoundIndex(name = "unique_daily_challenge_user", def = "{'userId': 1, 'challengeDate': 1}", unique = true)
public class ChallengeLog {

    @Id
    String id;

    @Indexed
    String userId;

    String challengeId;

    String challengeTitle;

    String recipeId;
    String recipeTitle;
    @Indexed
    String challengeDate;

    int bonusXp;

    @CreatedDate
    Instant completedAt;
}