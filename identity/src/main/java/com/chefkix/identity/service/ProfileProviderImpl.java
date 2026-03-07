package com.chefkix.identity.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.dto.response.internal.InternalBasicProfileResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .build();
    }

    @Override
    public CompletionResult updateAfterCompletion(CompletionRequest request) {
        // Map API-module DTO → identity-internal DTO
        InternalCompletionRequest internalReq = InternalCompletionRequest.builder()
                .userId(request.getUserId())
                .xpAmount(request.getXpAmount())
                .newBadges(request.getNewBadges())
                .build();

        RecipeCompletionResponse response = statisticsService.updateAfterCompletion(internalReq);

        // Map identity-internal response → API-module DTO
        return CompletionResult.builder()
                .userId(response.getUserId())
                .currentLevel(response.getCurrentLevel() != null ? response.getCurrentLevel() : 1)
                .currentXP(response.getCurrentXP() != null ? response.getCurrentXP().intValue() : 0)
                .currentXPGoal(response.getCurrentXPGoal() != null ? response.getCurrentXPGoal().intValue() : 0)
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
}
