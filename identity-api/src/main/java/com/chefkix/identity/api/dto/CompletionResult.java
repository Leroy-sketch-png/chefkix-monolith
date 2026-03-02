package com.chefkix.identity.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Result returned after updating user stats from cooking session completion.
 * Contains level-up tracking info for frontend celebration animations.
 * <p>
 * Unifies: identity's {@code RecipeCompletionResponse} and recipe-service's {@code ProfileUpdateResult}.
 * <p>
 * FIXED type mismatches:
 * <ul>
 *   <li>identity had {@code Double currentXP}, recipe had {@code Integer currentXP} → unified to {@code int}</li>
 *   <li>identity had {@code Double currentXPGoal}, recipe had {@code Integer currentXPGoal} → unified to {@code int}</li>
 * </ul>
 * All XP values are integers (no fractional XP in ChefKix).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletionResult {

    String userId;

    int currentLevel;

    int currentXP;

    int currentXPGoal;

    long completionCount;

    /** Whether this completion caused a level-up */
    boolean leveledUp;

    /** Level before this completion (only meaningful when {@code leveledUp} is true) */
    Integer oldLevel;

    /** Level after this completion (only meaningful when {@code leveledUp} is true) */
    Integer newLevel;

    /** XP remaining until next level */
    int xpToNextLevel;
}
