package com.chefkix.culinary.features.duel.dto.response;

import com.chefkix.culinary.features.duel.entity.DuelStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DuelResponse {
    String id;

    // Participants
    String challengerId;
    String challengerName;
    String challengerAvatar;
    String opponentId;
    String opponentName;
    String opponentAvatar;

    // Recipe
    String recipeId;
    String recipeTitle;
    String recipeCoverUrl;

    // State
    DuelStatus status;
    String message;

    // Scores (null until sessions complete)
    Integer challengerScore;
    Integer opponentScore;
    String winnerId;
    int bonusXp;

    // Sessions (null until cooking starts)
    String challengerSessionId;
    String opponentSessionId;

    // Deadlines
    Instant acceptDeadline;
    Instant cookDeadline;

    // Timestamps
    Instant createdAt;
    Instant acceptedAt;
    Instant completedAt;
}
