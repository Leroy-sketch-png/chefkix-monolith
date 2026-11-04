package com.chefkix.identity.api;

/**
 * Cross-module provider contract for user subscription and premium feature entitlements.
 */
public interface SubscriptionProvider {

    /**
     * Checks whether the user currently has an active premium subscription.
     *
     * @param userId the user ID to check
     * @return true if the user is an active premium subscriber, false otherwise
     */
    boolean isPremium(String userId);

    /**
     * Asserts that the specified user has an active premium subscription.
     * Throws an exception if the user is not premium.
     *
     * @param userId the user ID to check
     */
    void requirePremium(String userId);

    /**
     * Checks whether the user is entitled to a specific premium feature code.
     *
     * @param userId the user ID to check
     * @param featureCode the feature identifier (e.g. ADVANCED_ANALYTICS, CUSTOM_PROFILE_THEMES)
     * @return true if the feature is unlocked for this user, false otherwise
     */
    boolean hasFeature(String userId, String featureCode);
}
