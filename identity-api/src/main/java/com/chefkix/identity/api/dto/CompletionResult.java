package com.chefkix.identity.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
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

    boolean leveledUp;

    Integer oldLevel;

    Integer newLevel;

    int xpToNextLevel;
}
