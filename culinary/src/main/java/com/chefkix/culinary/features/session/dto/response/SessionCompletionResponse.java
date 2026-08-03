package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeRewardResult;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SessionCompletionResponse {
    private String sessionId;
    private String status;
private Integer baseXpAwarded;
private Integer recipeXpAwarded;
private Integer coOpBonusXp;
private Integer pendingXp;
private Recipe.XpBreakdown xpBreakdown;
private List<ChallengeRewardResult> completedChallengeRewards;
private String xpDeliveryStatus;
    private String message;
    private LocalDateTime postDeadline;
    
private Boolean leveledUp;
private Integer oldLevel;
private Integer newLevel;
private Integer currentXp;
private Integer xpToNextLevel;

private Double xpMultiplier;
private String xpMultiplierReason;

private List<String> newAchievements;
}
