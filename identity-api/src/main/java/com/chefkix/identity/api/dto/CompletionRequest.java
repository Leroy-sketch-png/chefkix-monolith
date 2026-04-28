package com.chefkix.identity.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request to award XP and badges after a cooking session completion.
 * <p>
 * Unifies the duplicated DTOs:
 * <ul>
 *   <li>{@code chefkix-be} → InternalCompletionRequest ({@code Integer xpAmount})</li>
 *   <li>culinary module → InternalCompletionRequest ({@code int xpAmount})</li>
 * </ul>
 * <p>
 * FIXED: Uses {@code int} (not Integer) for xpAmount — XP is always required, never null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletionRequest {

    String userId;

    int xpAmount;

    String recipeId;

    boolean challengeCompleted;

    List<String> newBadges;

    /**
     * Deterministic idempotency key for this completion.
     * Used to prevent double XP award when sync path succeeds but fallback Kafka also fires.
     * Format: "xp:COOKING_SESSION:{sessionId}" — same key used by the fallback XpRewardEvent.
     */
    String idempotencyKey;
}
