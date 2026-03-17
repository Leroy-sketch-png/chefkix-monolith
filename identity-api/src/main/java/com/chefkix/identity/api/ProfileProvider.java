package com.chefkix.identity.api;

import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;

import java.time.Instant;
import java.util.List;

/**
 * Cross-module contract for identity/profile operations.
 * <p>
 * Implemented by {@code identity} module, consumed by {@code culinary}, {@code social}, {@code notification}.
 * Replaces the old Feign clients: ProfileClient (in recipe, post, chat services).
 */
public interface ProfileProvider {

    /**
     * Lightweight profile lookup (6 core fields).
     * Replaces: {@code GET /auth/internal/{userId}} across recipe, post, chat services.
     *
     * @param userId the user's ID
     * @return basic profile info, never null (throws if not found)
     */
    BasicProfileInfo getBasicProfile(String userId);

    /**
     * Award XP and badges after cooking session completion.
     * Replaces: {@code PUT /auth/update_completion} (identity's Feign endpoint consumed by recipe-service).
     *
     * @param request completion data (userId, xpAmount, newBadges)
     * @return updated stats including level-up info for frontend celebration
     */
    CompletionResult updateAfterCompletion(CompletionRequest request);

    /**
     * Get the IDs of users that the given user is following (mutual followers only).
     * Replaces: {@code GET /auth/{userId}/friends} (identity's Feign endpoint consumed by recipe-service).
     * Used for "friends only" recipe visibility filtering.
     *
     * @param userId the user whose friend list to retrieve
     * @return list of mutual follower user IDs (never null, may be empty)
     */
    List<String> getFriendIds(String userId);

    /**
     * Get the IDs of all users that the given user is following (one-directional).
     * Used for personalized "Following" feed — shows posts from everyone the user follows.
     *
     * @param userId the user whose following list to retrieve
     * @return list of followed user IDs (never null, may be empty)
     */
    List<String> getFollowingIds(String userId);

    /**
     * Update user's online/offline status.
     * Replaces: {@code PUT /internal/users/{userId}/status} (identity's endpoint consumed by chat-service).
     *
     * @param userId  the user whose status to update
     * @param isOnline true if user is online, false if offline
     */
    void updateUserOnlineStatus(String userId, boolean isOnline);

    /**
     * Get the account creation timestamp for a user.
     * Used by culinary module for account-age-based rate limiting.
     *
     * @param userId the user whose creation date to retrieve
     * @return account creation instant (never null)
     */
    Instant getAccountCreatedAt(String userId);

    /**
     * verify user's authentication
     * @param userId the user's ID
     * @return boolean
     */
    boolean verifyUserPassword(String userId, String confirmationPassword);
}
