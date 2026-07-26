package com.chefkix.identity.api;

import com.chefkix.identity.api.dto.AchievementStatsSnapshot;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.identity.api.dto.PlanningPreferences;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 */
public interface ProfileProvider {

    /**
     *
     */
    BasicProfileInfo getBasicProfile(String userId);

    /**
     *
     */
    CompletionResult updateAfterCompletion(CompletionRequest request);

    /**
     *
     */
    List<String> getFriendIds(String userId);

    /**
     *
     */
    List<String> getFollowingIds(String userId);

    /**
     *
     */
    void updateUserOnlineStatus(String userId, boolean isOnline);

    /**
     *
     */
    Instant getAccountCreatedAt(String userId);

    /**
     */
    boolean verifyUserPassword(String userId, String confirmationPassword);

    /**
     *
     */
    boolean isBlocked(String userId1, String userId2);

    /**
     *
     */
    List<String> getInvisibleUserIds(String userId);

    /**
     *
     */
    boolean isShowCookingActivity(String userId);

    /**
     *
     */
    AchievementStatsSnapshot getAchievementStats(String userId);

    /**
     *
     */
    List<String> getUserPreferences(String userId);

    /**
     */
    PlanningPreferences getPlanningPreferences(String userId);

    /**
     *
     */
    int getUserLevel(String userId);

    /**
     *
     */
    Map<String, Double> getBehavioralPostWeights(String userId);

    /**
     *
     */
    List<String> getRecentSearchQueries(String userId);

    /**
     *
     */
    long deleteUserEventData(String userId);
}
