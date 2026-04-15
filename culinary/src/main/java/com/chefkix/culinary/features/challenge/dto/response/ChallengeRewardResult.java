package com.chefkix.culinary.features.challenge.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChallengeRewardResult {
    private boolean completed;    // True if just completed
    private int bonusXp;          // Bonus XP amount
    private String challengeTitle; // Challenge name (for displaying popup)
}