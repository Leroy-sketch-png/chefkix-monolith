package com.chefkix.identity.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.AchievementStatsSnapshot;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.dto.response.internal.InternalBasicProfileResponse;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.UserEvent;
import com.chefkix.identity.enums.TrackingEventType;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.UserSettingsRepository;
import com.chefkix.identity.repository.UserEventRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the cross-module {@link ProfileProvider} contract.
 * <p>
 * Delegates to internal identity services, mapping between API-module DTOs
 * and identity-module internal DTOs. This replaces the Feign clients
 * (ProfileClient) that recipe, post, and chat services used to call.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileProviderImpl implements ProfileProvider {

    ProfileService profileService;
    StatisticsService statisticsService;
    SocialService socialService;
    UserStatusService userStatusService;
    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserSettingsRepository userSettingsRepository;
    UserEventRepository userEventRepository;
    private final KeycloakService keycloakService;
    BlockService blockService;

    @Override
    public BasicProfileInfo getBasicProfile(String userId) {
        InternalBasicProfileResponse response = profileService.getBasicProfile(userId);
        return BasicProfileInfo.builder()
                .userId(response.getUserId())
                .username(response.getUsername())
                .displayName(response.getDisplayName())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .avatarUrl(response.getAvatarUrl())
                .verified(response.isVerified())
                .build();
    }

    @Override
    public CompletionResult updateAfterCompletion(CompletionRequest request) {
        // Map API-module DTO → identity-internal DTO
        InternalCompletionRequest internalReq = InternalCompletionRequest.builder()
                .userId(request.getUserId())
                .xpAmount(request.getXpAmount())
            .recipeId(request.getRecipeId())
            .challengeCompleted(request.isChallengeCompleted())
                .newBadges(request.getNewBadges())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        RecipeCompletionResponse response = statisticsService.updateAfterCompletion(internalReq);

        // Map identity-internal response → API-module DTO
        return CompletionResult.builder()
                .userId(response.getUserId())
                .currentLevel(response.getCurrentLevel() != null ? response.getCurrentLevel() : 1)
                .currentXP(response.getCurrentXP() != null ? response.getCurrentXP() : 0)
                .currentXPGoal(response.getCurrentXPGoal() != null ? response.getCurrentXPGoal() : 0)
                .completionCount(response.getCompletionCount() != null ? response.getCompletionCount() : 0)
                .leveledUp(response.getLeveledUp() != null && response.getLeveledUp())
                .oldLevel(response.getOldLevel())
                .newLevel(response.getNewLevel())
                .xpToNextLevel(response.getXpToNextLevel() != null ? response.getXpToNextLevel() : 0)
                .build();
    }

    @Override
    public List<String> getFriendIds(String userId) {
        return socialService.getFriendIds(userId);
    }

    @Override
    public List<String> getFollowingIds(String userId) {
        return socialService.getFollowingIds(userId);
    }

    @Override
    public void updateUserOnlineStatus(String userId, boolean isOnline) {
        if (isOnline) {
            userStatusService.setUserOnline(userId);
        } else {
            userStatusService.setUserOffline(userId);
        }
    }

    @Override
    public Instant getAccountCreatedAt(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getCreatedAt() == null) {
            // Fallback for legacy users without createdAt — treat as old account (no restriction)
            return Instant.EPOCH;
        }
        return user.getCreatedAt().toInstant(ZoneOffset.UTC);
    }

    @Override
    public boolean isBlocked(String userId1, String userId2) {
        return blockService.isBlocked(userId1, userId2);
    }

    @Override
    public List<String> getInvisibleUserIds(String userId) {
        return blockService.getInvisibleUserIds(userId);
    }

    @Override
    public boolean verifyUserPassword(String userName, String confirmationPassword) {

        return keycloakService.verifyPassword(userName, confirmationPassword);
    }

    @Override
    public boolean isShowCookingActivity(String userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(settings -> settings.getPrivacy() != null
                        && settings.getPrivacy().getShowCookingActivity() != null
                        ? settings.getPrivacy().getShowCookingActivity()
                        : true)
                .orElse(true); // Default: broadcast cooking activity
    }

    @Override
    public AchievementStatsSnapshot getAchievementStats(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        Statistics stats = profile.getStatistics();
        if (stats == null) {
            return AchievementStatsSnapshot.builder()
                    .streakCount(0)
                    .followerCount(0)
                    .totalRecipesPublished(0)
                    .build();
        }
        return AchievementStatsSnapshot.builder()
                .streakCount(stats.getStreakCount() != null ? stats.getStreakCount() : 0)
                .followerCount(stats.getFollowerCount() != null ? stats.getFollowerCount() : 0)
                .totalRecipesPublished(stats.getTotalRecipesPublished() != null ? stats.getTotalRecipesPublished() : 0)
                .build();
    }

    @Override
    public List<String> getUserPreferences(String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getPreferences)
                .map(prefs -> prefs != null ? prefs : List.<String>of())
                .orElse(List.of());
    }

    @Override
    public int getUserLevel(String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getStatistics)
                .map(stats -> stats.getCurrentLevel() != null ? stats.getCurrentLevel() : 1)
                .orElse(1);
    }

    @Override
    public Map<String, Double> getBehavioralPostWeights(String userId) {
        List<UserEvent> events = userEventRepository.findByUserIdAndEventTypeInOrderByTimestampDesc(
                userId,
                List.of(TrackingEventType.RECIPE_VIEWED, TrackingEventType.POST_DWELLED,
                        TrackingEventType.POST_COMMENTED, TrackingEventType.RECIPE_CREATED));

        Map<String, Double> postWeights = new HashMap<>();
        for (UserEvent event : events) {
            if (event.getEntityId() == null) continue;
            double weight = switch (event.getEventType()) {
                case RECIPE_VIEWED -> 0.5;
                case POST_DWELLED -> computeDwellWeight(event);
                case POST_COMMENTED -> 1.8; // commenting shows strong engagement
                case RECIPE_CREATED -> 2.5; // creating a recipe = strongest intent signal
                default -> 0.0;
            };
            if (weight > 0) {
                postWeights.merge(event.getEntityId(), weight, (a, b) -> a + b);
            }
        }
        return postWeights;
    }

    /**
     * Graduated dwell weight: longer dwell = stronger interest signal.
     * 2-5s = casual browse (0.75), 5-10s = engaged read (1.5), 10s+ = deep interest (2.5)
     */
    private double computeDwellWeight(UserEvent event) {
        if (event.getMetadata() == null) return 1.5; // fallback for legacy events
        Object dwellMsObj = event.getMetadata().get("dwellMs");
        if (dwellMsObj == null) return 1.5;

        double dwellMs;
        if (dwellMsObj instanceof Number n) {
            dwellMs = n.doubleValue();
        } else {
            try {
                dwellMs = Double.parseDouble(dwellMsObj.toString());
            } catch (NumberFormatException e) {
                return 1.5;
            }
        }

        if (dwellMs < 5_000) return 0.75;   // 2-5s: casual browse
        if (dwellMs < 10_000) return 1.5;    // 5-10s: engaged read
        return 2.5;                           // 10s+: deep interest
    }

    @Override
    public List<String> getRecentSearchQueries(String userId) {
        List<UserEvent> searchEvents = userEventRepository.findByUserIdAndEventTypeInOrderByTimestampDesc(
                userId,
                List.of(TrackingEventType.RECIPE_SEARCH));

        return searchEvents.stream()
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("query"))
                .map(e -> e.getMetadata().get("query").toString().toLowerCase().trim())
                .filter(q -> q.length() >= 2)
                .limit(50)
                .toList();
    }

    @Override
    public long deleteUserEventData(String userId) {
        return userEventRepository.deleteByUserId(userId);
    }
}
