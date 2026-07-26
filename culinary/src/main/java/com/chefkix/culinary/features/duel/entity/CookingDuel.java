package com.chefkix.culinary.features.duel.entity;

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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "cooking_duels")
@CompoundIndexes({
        @CompoundIndex(name = "challenger_status_idx", def = "{'challengerId': 1, 'status': 1}"),
        @CompoundIndex(name = "opponent_status_idx", def = "{'opponentId': 1, 'status': 1}")
})
public class CookingDuel {

    @Id
    String id;

    @Indexed
    String challengerId;

    @Indexed
    String opponentId;

    String recipeId;
    String recipeTitle;
    String recipeCoverUrl;

    @Builder.Default
    DuelStatus status = DuelStatus.PENDING;

    String message;

    String challengerSessionId;
    String opponentSessionId;

    Integer challengerScore;
    Integer opponentScore;

String winnerId;

    @Builder.Default
int bonusXp = 50;

Instant acceptDeadline;
Instant cookDeadline;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;

    Instant acceptedAt;
    Instant completedAt;
}
