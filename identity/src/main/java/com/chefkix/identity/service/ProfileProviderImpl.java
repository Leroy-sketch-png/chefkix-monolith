package com.chefkix.identity.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.dto.response.internal.InternalBasicProfileResponse;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
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
    UserRepository userRepository;
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
                .build();
    }

    @Override
    public CompletionResult updateAfterCompletion(CompletionRequest request) {
        // Map API-module DTO → identity-internal DTO
        InternalCompletionRequest internalReq = InternalCompletionRequest.builder()
                .userId(request.getUserId())
                .xpAmount(request.getXpAmount())
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
}
