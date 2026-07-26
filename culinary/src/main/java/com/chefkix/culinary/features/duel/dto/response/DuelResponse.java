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

    String challengerId;
    String challengerName;
    String challengerAvatar;
    String opponentId;
    String opponentName;
    String opponentAvatar;

    String recipeId;
    String recipeTitle;
    String recipeCoverUrl;

    DuelStatus status;
    String message;

    Integer challengerScore;
    Integer opponentScore;
    String winnerId;
    int bonusXp;

    String challengerSessionId;
    String opponentSessionId;

    Instant acceptDeadline;
    Instant cookDeadline;

    Instant createdAt;
    Instant acceptedAt;
    Instant completedAt;
}
