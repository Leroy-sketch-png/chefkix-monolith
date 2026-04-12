package com.chefkix.identity.api;

import com.chefkix.identity.api.dto.AchievementStatsSnapshot;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    /**
     * Check if there is a block relationship between two users (either direction).
     * Used by social module (chat, posts, comments) to enforce block boundaries.
     *
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return true if either user has blocked the other
     */
    boolean isBlocked(String userId1, String userId2);

    /**
     * Get all user IDs that should be invisible to the given user.
     * Includes users they've blocked AND users who blocked them.
     * Used by social module (feeds, leaderboards) for batch filtering.
     *
     * @param userId the user whose invisible list to retrieve
     * @return list of invisible user IDs (never null, may be empty)
     */
    List<String> getInvisibleUserIds(String userId);

    /**
     * Check if user has opted-in to broadcasting cooking activity (showCookingActivity).
     * Used by culinary module to decide whether to auto-create a RECENT_COOK post.
     * Defaults to true when user has no settings.
     *
     * @param userId the user's ID
     * @return true if cooking activity should be broadcast to followers
     */
    boolean isShowCookingActivity(String userId);

    /**
     * Get lightweight stats snapshot for achievement evaluation.
     * Used by culinary module's AchievementService to check streak days,
     * follower count, and recipe count without a full profile load.
     *
     * @param userId the user's ID
     * @return stats snapshot (never null)
     */
    AchievementStatsSnapshot getAchievementStats(String userId);

    /**
     * Get user's cuisine/interest preferences from onboarding or profile settings.
     * Used by culinary module for Tonight's Pick personalization.
     *
     * @param userId the user's ID
     * @return list of preference IDs (e.g., "italian", "bbq", "vegan"), never null
     */
    List<String> getUserPreferences(String userId);

    /**
     * Get user's current gamification level.
     * Used by culinary module for difficulty-appropriate recipe recommendations.
     *
     * @param userId the user's ID
     * @return current level (1+), defaults to 1 for new users
     */
    int getUserLevel(String userId);

    /**
     * Get post IDs from recent behavioral events with graduated weights.
     * 5-signal: views (0.5x), dwell (0.75-2.5x graduated), comments (1.8x), creation (2.5x).
     * Used by social module's feed algorithm to enrich the taste vector
     * beyond likes/saves to include behavioral signals.
     *
     * @param userId the user whose behavioral signals to extract
     * @return map of postId → weight, never null
     */
    Map<String, Double> getBehavioralPostWeights(String userId);

    /**
     * Get recent search queries for taste vector enrichment.
     * Extracts query terms from RECIPE_SEARCH events which indicate
     * user intent even when they don't interact with results.
     *
     * @param userId the user whose search history to extract
     * @return list of recent search query strings (last 50), never null
     */
    List<String> getRecentSearchQueries(String userId);

    /**
     * Delete all event tracking data for a user (GDPR compliance).
     *
     * @param userId the user whose event data to delete
     * @return number of events deleted
     */
    long deleteUserEventData(String userId);
}
