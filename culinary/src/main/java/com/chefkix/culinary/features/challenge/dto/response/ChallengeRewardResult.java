package com.chefkix.culinary.features.challenge.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChallengeRewardResult {
    private boolean completed;    // True nếu vừa hoàn thành xong
    private int bonusXp;          // Số XP thưởng
    private String challengeTitle; // Tên Challenge (để hiện popup)
}