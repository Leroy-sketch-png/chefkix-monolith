package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a gamification milestone is reached (level-up, badge earned, XP summary).
 * <p>
 * Producer: identity module. Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("GAMIFICATION_ACTION")
public class GamificationNotificationEvent extends BaseEvent {

    private String displayName;
    private double xpEarned;
    private double totalXp;
    private boolean leveledUp;
    private int previousLevel;
    private int newLevel;
    private String newTitle;
    private List<String> newBadges;
    private String source;
    private String recipeId;
    private String sessionId;

    @Builder
    public GamificationNotificationEvent(String userId, String displayName,
                                         double xpEarned, double totalXp,
                                         boolean leveledUp, int previousLevel,
                                         int newLevel, String newTitle,
                                         List<String> newBadges, String source,
                                         String recipeId, String sessionId) {
        super("GAMIFICATION_ACTION", userId);
        this.displayName = displayName;
        this.xpEarned = xpEarned;
        this.totalXp = totalXp;
        this.leveledUp = leveledUp;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.newTitle = newTitle;
        this.newBadges = newBadges;
        this.source = source;
        this.recipeId = recipeId;
        this.sessionId = sessionId;
    }
}
