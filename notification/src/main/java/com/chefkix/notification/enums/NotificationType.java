package com.chefkix.notification.enums;

public enum NotificationType {
    // Social - Instagram model (follow-based)
    FOLLOW,
    NEW_FOLLOWER,

    // Posts
    POST_LIKE,
    POST_COMMENT,
    RECIPE_LIKED,
    USER_MENTION,

    // Gamification
    XP_AWARDED,
    LEVEL_UP,
    BADGE_EARNED,
    CREATOR_BONUS,

    // Reminders (scheduled)
    STREAK_WARNING,
    POST_DEADLINE,
    CHALLENGE_AVAILABLE,
    CHALLENGE_REMINDER,
    WEEKEND_NUDGE,
    PANTRY_EXPIRING,

    // Co-Cooking (spec 24-advanced-multiplayer.txt)
    ROOM_INVITE,
    CO_CHEF_TAGGED,

    // Cooking Duels (1v1)
    DUEL_INVITE,
    DUEL_ACCEPTED,
    DUEL_DECLINED,
    DUEL_COMPLETED,
    DUEL_EXPIRED,

    // Group
    JOIN_REQUESTED,
    MEMBER_JOINED,
    JOIN_REQUEST_APPROVED,
}
