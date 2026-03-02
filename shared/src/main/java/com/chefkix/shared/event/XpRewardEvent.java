package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when XP is awarded (cooking completion, post bonus, challenge).
 * <p>
 * Producer: culinary module (recipe/session). Consumer: identity module (statistics).
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("XP_REWARDED_ACTION")
public class XpRewardEvent extends BaseEvent {

    private String sessionId;
    private double amount;
    private String source;
    private String description;
    private String postId;
    private String recipeId;
    private List<String> badges;
    private boolean challengeCompleted;

    @Builder
    public XpRewardEvent(String userId, String sessionId, double amount,
                         String source, String postId, String description,
                         String recipeId, List<String> badges, boolean challengeCompleted) {
        super("XP_REWARDED_ACTION", userId);
        this.sessionId = sessionId;
        this.amount = amount;
        this.source = source;
        this.postId = postId;
        this.description = description;
        this.recipeId = recipeId;
        this.badges = badges;
        this.challengeCompleted = challengeCompleted;
    }
}
