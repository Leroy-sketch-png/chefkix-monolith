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
 * 1-v-1 cooking duel between two users on a specific recipe.
 * <p>
 * Flow: PENDING -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 *       PENDING -> DECLINED / EXPIRED / CANCELLED
 * <p>
 * Each participant cooks the recipe via normal CookingSession.
 * On session completion, DuelService links the session to the duel
 * and computes scores. When both sessions are complete (or deadline passes),
 * a winner is determined.
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

    // Participants
    @Indexed
    String challengerId;

    @Indexed
    String opponentId;

    // Recipe to cook
    String recipeId;
    String recipeTitle;
    String recipeCoverUrl;

    // Status
    @Builder.Default
    DuelStatus status = DuelStatus.PENDING;

    // Optional message from challenger ("Think you can beat me at Carbonara?")
    String message;

    // Linked cooking sessions (null until each participant cooks)
    String challengerSessionId;
    String opponentSessionId;

    // Scores (computed from session data: time accuracy, step completion, etc.)
    Integer challengerScore;
    Integer opponentScore;

    // Winner
    String winnerId; // null if tie or not completed

    // XP stakes
    @Builder.Default
    int bonusXp = 50; // Winner gets this bonus on top of normal session XP

    // Deadline: opponent must accept within 48h, both must cook within 24h of acceptance
    Instant acceptDeadline;  // challengerId created + 48h
    Instant cookDeadline;    // accepted + 24h

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;

    Instant acceptedAt;
    Instant completedAt;
}
