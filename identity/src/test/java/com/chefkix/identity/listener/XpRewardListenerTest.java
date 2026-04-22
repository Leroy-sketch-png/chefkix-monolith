package com.chefkix.identity.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.service.StatisticsService;
import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.service.KafkaIdempotencyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XpRewardListenerTest {

    private static final String XP_SCOPE = "xp-delivery";

    @Mock
    private StatisticsService statisticsService;
    @Mock
    private KafkaIdempotencyService idempotencyService;

    private XpRewardListener listener;

    @BeforeEach
    void setUp() {
        listener = new XpRewardListener(statisticsService, idempotencyService);
    }

    @Test
    void listenXpRewardDeliverySkipsDuplicateEvent() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .amount(25)
                .source("SESSION_COMPLETE")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(false);

        listener.listenXpRewardDelivery(event);

        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

        @Test
        void listenXpRewardDeliverySkipsNullEvent() {
                listener.listenXpRewardDelivery(null);

                verify(idempotencyService, never()).tryProcess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
                verify(statisticsService, never()).applyCreatorReward(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyDouble());
                verify(statisticsService, never()).rewardSocialXp(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyString());
                verify(statisticsService, never()).rewardXpFull(
                                org.mockito.ArgumentMatchers.anyString(),
                                org.mockito.ArgumentMatchers.anyDouble(),
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.anyBoolean(),
                                org.mockito.ArgumentMatchers.anyString(),
                                org.mockito.ArgumentMatchers.anyString(),
                                org.mockito.ArgumentMatchers.anyString());
        }

    @Test
    void listenXpRewardDeliverySkipsEventWithMissingEventId() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .amount(25)
                .source("COOKING_SESSION")
                .build();
        event.setEventId(" ");

        listener.listenXpRewardDelivery(event);

        verify(idempotencyService, never()).tryProcess(event.getEventId(), XP_SCOPE);
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliverySkipsEventWithBlankUserId() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .amount(25)
                .source("COOKING_SESSION")
                .build();
        event.setUserId(" ");

        listener.listenXpRewardDelivery(event);

        verify(idempotencyService, never()).tryProcess(event.getEventId(), XP_SCOPE);
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliveryThrowsOnNegativeXpBeforeIdempotency() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .amount(-5)
                .source("COOKING_SESSION")
                .build();

        assertThrows(IllegalArgumentException.class, () -> listener.listenXpRewardDelivery(event));

        verify(idempotencyService, never()).tryProcess(event.getEventId(), XP_SCOPE);
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliverySkipsZeroXpEventWithoutBadgesOrChallenge() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .amount(0)
                .source("SESSION_COMPLETE")
                .build();

        listener.listenXpRewardDelivery(event);

        verify(idempotencyService, never()).tryProcess(event.getEventId(), XP_SCOPE);
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliveryProcessesZeroXpEventWhenBadgesExist() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .amount(0)
                .source("COOKING_SESSION")
                .badges(List.of("broth-master"))
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);

        listener.listenXpRewardDelivery(event);

        verify(statisticsService).rewardXpFull(
                event.getUserId(),
                event.getAmount(),
                event.getBadges(),
                event.isChallengeCompleted(),
                event.getRecipeId(),
                event.getSource(),
                event.getSessionId());
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
    }

        @Test
        void listenXpRewardDeliveryProcessesZeroXpEventWhenChallengeCompletes() {
                XpRewardEvent event = XpRewardEvent.builder()
                                .userId("user-1")
                                .sessionId("session-1")
                                .recipeId("recipe-1")
                                .amount(0)
                                .source("COOKING_SESSION")
                                .challengeCompleted(true)
                                .build();

                when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);

                listener.listenXpRewardDelivery(event);

                verify(statisticsService).rewardXpFull(
                                event.getUserId(),
                                event.getAmount(),
                                event.getBadges(),
                                event.isChallengeCompleted(),
                                event.getRecipeId(),
                                event.getSource(),
                                event.getSessionId());
                verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
                verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        }

    @Test
    void listenXpRewardDeliveryReleasesIdempotencyOnRetryableFailure() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .amount(25)
                .source("SESSION_COMPLETE")
                .badges(List.of("streak-3"))
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);
        RuntimeException failure = new RuntimeException("stats write failed");
        org.mockito.Mockito.doThrow(failure)
                .when(statisticsService)
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());

        assertThrows(RuntimeException.class, () -> listener.listenXpRewardDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), XP_SCOPE);
    }

    @Test
    void listenXpRewardDeliveryKeepsIdempotencyForMissingUserProfile() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("deleted-user")
                .sessionId("session-1")
                .amount(25)
                .source("SESSION_COMPLETE")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);
        AppException missingUser = new AppException(ErrorCode.USER_NOT_FOUND);
        org.mockito.Mockito.doThrow(missingUser)
                .when(statisticsService)
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());

        listener.listenXpRewardDelivery(event);

        verify(idempotencyService, never()).removeProcessed(event.getEventId(), XP_SCOPE);
    }

    @Test
    void listenXpRewardDeliveryRoutesSocialXpWithoutFullRewardPath() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .amount(5)
                .source("SOCIAL_LIKE")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);

        listener.listenXpRewardDelivery(event);

        verify(statisticsService).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliveryRoutesCreatorBonusWithoutCookOrSocialPaths() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("creator-1")
                .recipeId("recipe-1")
                .amount(11)
                .source("CREATOR_BONUS")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);

        listener.listenXpRewardDelivery(event);

        verify(statisticsService).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), XP_SCOPE);
    }

    @Test
    void listenXpRewardDeliveryReleasesIdempotencyOnRetryableCreatorBonusFailure() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("creator-1")
                .recipeId("recipe-1")
                .amount(11)
                .source("CREATOR_BONUS")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);
        AppException failure = new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        org.mockito.Mockito.doThrow(failure)
                .when(statisticsService)
                .applyCreatorReward(event.getUserId(), event.getAmount());

        assertThrows(AppException.class, () -> listener.listenXpRewardDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), XP_SCOPE);
        verify(statisticsService).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliveryKeepsIdempotencyForMissingProfileOnCreatorBonus() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("creator-1")
                .recipeId("recipe-1")
                .amount(11)
                .source("CREATOR_BONUS")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);
        AppException missingProfile = new AppException(ErrorCode.PROFILE_NOT_FOUND);
        org.mockito.Mockito.doThrow(missingProfile)
                .when(statisticsService)
                .applyCreatorReward(event.getUserId(), event.getAmount());

        listener.listenXpRewardDelivery(event);

        verify(idempotencyService, never()).removeProcessed(event.getEventId(), XP_SCOPE);
        verify(statisticsService).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(statisticsService, never())
                .rewardXpFull(
                        event.getUserId(),
                        event.getAmount(),
                        event.getBadges(),
                        event.isChallengeCompleted(),
                        event.getRecipeId(),
                        event.getSource(),
                        event.getSessionId());
    }

    @Test
    void listenXpRewardDeliveryRoutesCookXpFullWithRecipeBadgesAndChallengeContext() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .amount(39)
                .source("COOKING_SESSION")
                .badges(List.of("streak-3", "broth-master"))
                .challengeCompleted(true)
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);

        listener.listenXpRewardDelivery(event);

        verify(statisticsService).rewardXpFull(
                event.getUserId(),
                event.getAmount(),
                event.getBadges(),
                event.isChallengeCompleted(),
                event.getRecipeId(),
                event.getSource(),
                event.getSessionId());
        verify(statisticsService, never()).applyCreatorReward(event.getUserId(), event.getAmount());
        verify(statisticsService, never()).rewardSocialXp(event.getUserId(), event.getAmount(), event.getSource());
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), XP_SCOPE);
    }

    @Test
    void listenXpRewardDeliveryPreservesLinkPostSourceAndSessionContext() {
        XpRewardEvent event = XpRewardEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .amount(70)
                .source("LINKING_POST")
                .badges(List.of("broth-master"))
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), XP_SCOPE)).thenReturn(true);

        listener.listenXpRewardDelivery(event);

        verify(statisticsService).rewardXpFull(
                event.getUserId(),
                event.getAmount(),
                event.getBadges(),
                event.isChallengeCompleted(),
                event.getRecipeId(),
                event.getSource(),
                event.getSessionId());
    }
}