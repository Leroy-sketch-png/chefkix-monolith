package com.chefkix.identity.api;

/**
 * Cross-module contract for checking user notification preferences.
 * <p>
 * Implemented by {@code identity} module, consumed by {@code notification} module.
 * Allows the notification service to respect user preferences before creating/broadcasting.
 */
public interface NotificationPreferencesProvider {

    /**
     * Check if a specific notification category is enabled for a user.
     * Maps notification type categories to user's in-app notification toggles.
     *
     * @param userId   the user whose preferences to check
     * @param category the notification category (e.g., "social", "followers", "xpAndLevelUps", "badges",
     *                 "postDeadline", "streakWarning", "dailyChallenge")
     * @return true if notifications of this category are enabled (defaults to true if settings don't exist)
     */
    boolean isNotificationEnabled(String userId, String category);
}
