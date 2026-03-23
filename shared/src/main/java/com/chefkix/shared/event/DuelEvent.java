package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired for cooking duel lifecycle events (invite, accept, complete, etc.).
 * <p>
 * Producer: culinary module. Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("DUEL_ACTION")
public class DuelEvent extends BaseEvent {

    private String duelId;
    private String duelAction;  // "INVITE", "ACCEPTED", "DECLINED", "COMPLETED", "EXPIRED"
    private String challengerName;
    private String opponentName;
    private String recipeTitle;
    private String recipeCoverUrl;
    private String message;
    private String winnerId;
    private int bonusXp;

    @Builder
    public DuelEvent(String userId, String duelId, String duelAction,
                     String challengerName, String opponentName,
                     String recipeTitle, String recipeCoverUrl,
                     String message, String winnerId, int bonusXp) {
        super("DUEL_ACTION", userId);
        this.duelId = duelId;
        this.duelAction = duelAction;
        this.challengerName = challengerName;
        this.opponentName = opponentName;
        this.recipeTitle = recipeTitle;
        this.recipeCoverUrl = recipeCoverUrl;
        this.message = message;
        this.winnerId = winnerId;
        this.bonusXp = bonusXp;
    }
}
