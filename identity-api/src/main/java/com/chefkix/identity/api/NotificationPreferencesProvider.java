package com.chefkix.identity.api;

/**
 */
public interface NotificationPreferencesProvider {

    /**
     *
     */
    boolean isNotificationEnabled(String userId, String category);
}
