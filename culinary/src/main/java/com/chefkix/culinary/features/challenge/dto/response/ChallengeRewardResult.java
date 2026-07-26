package com.chefkix.culinary.features.challenge.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChallengeRewardResult {
private boolean completed;
private int bonusXp;
private String challengeTitle;
}