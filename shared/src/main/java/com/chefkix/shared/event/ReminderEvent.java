package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired for reminder notifications (streak at risk, post deadline, challenge ending).
 * <p>
 * Producer: culinary module (scheduler), identity module (scheduler).
 * Consumer: notification module.
 * <p>
 * NOTE: {@code reminderType} uses String (not enum) so that shared module
 * stays decoupled from notification-specific enum types. Notification module
 * maps these strings to its own {@code NotificationType} enum on consumption.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("REMINDER_ACTION")
public class ReminderEvent extends BaseEvent {

    /** Reminder category, e.g. "STREAK_AT_RISK", "POST_DEADLINE", "CHALLENGE_ENDING", "ROOM_INVITE". */
    private String reminderType;
    private String displayName;
    private String content;
    private ReminderPriority priority;
    private Integer streakCount;
    private Integer hoursRemaining;
    private String sessionId;
    private String recipeTitle;
    private Integer daysRemaining;
    private String challengeCategory;
    /** Co-cooking room code — used for ROOM_INVITE deep-link (/cook-together?roomCode=XXX). */
    private String roomCode;

    @Builder
    public ReminderEvent(String userId, String displayName, String reminderType,
                         String content, ReminderPriority priority,
                         Integer streakCount, Integer hoursRemaining,
                         String sessionId, String recipeTitle,
                         Integer daysRemaining, String challengeCategory,
                         String roomCode) {
        super("REMINDER_ACTION", userId);
        this.displayName = displayName;
        this.reminderType = reminderType;
        this.content = content;
        this.priority = priority;
        this.streakCount = streakCount;
        this.hoursRemaining = hoursRemaining;
        this.sessionId = sessionId;
        this.recipeTitle = recipeTitle;
        this.daysRemaining = daysRemaining;
        this.challengeCategory = challengeCategory;
        this.roomCode = roomCode;
    }

    public enum ReminderPriority {
        NORMAL,
        HIGH,
        CRITICAL
    }
}
