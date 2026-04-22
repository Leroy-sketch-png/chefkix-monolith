package com.chefkix.culinary.features.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeRewardResult;
import com.chefkix.culinary.features.challenge.service.ChallengeService;
import com.chefkix.culinary.features.duel.service.DuelService;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import com.chefkix.culinary.features.session.dto.request.CompleteSessionRequest;
import com.chefkix.culinary.features.session.dto.request.SessionLinkingRequest;
import com.chefkix.culinary.features.session.dto.response.SessionCompletionResponse;
import com.chefkix.culinary.features.session.dto.response.SessionLinkingResponse;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.mapper.CookingSessionMapper;
import com.chefkix.culinary.features.session.repository.ActiveCookingRedisRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.social.api.PostProvider;
import com.chefkix.social.api.dto.PostLinkInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CookingSessionServiceTest {

    @Mock
    private CookingSessionRepository sessionRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private CookingSessionMapper sessionMapper;
    @Mock
    private ChallengeService challengeService;
    @Mock
    private RecipeHelper helper;
    @Mock
    private ProfileProvider profileProvider;
    @Mock
    private PostProvider postProvider;
    @Mock
    private CookingRoomRedisRepository roomRepository;
    @Mock
    private ActiveCookingRedisRepository activeCookingRepository;
    @Mock
    private com.chefkix.culinary.features.achievement.service.AchievementService achievementService;
    @Mock
    private DuelService duelService;

    @InjectMocks
    private CookingSessionService cookingSessionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markLinkedPostDeletedTransitionsPostedSessions() {
        LocalDateTime linkedAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(2);
        CookingSession session = CookingSession.builder()
                .id("session-1")
                .status(SessionStatus.POSTED)
                .postId("post-1")
                .baseXpAwarded(30.0)
                .remainingXpAwarded(70.0)
            .linkedAt(linkedAt)
                .build();

        when(sessionRepository.findAllByPostIdAndStatus("post-1", SessionStatus.POSTED))
                .thenReturn(List.of(session));

        int updated = cookingSessionService.markLinkedPostDeleted("post-1");

        assertThat(updated).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.POST_DELETED);
        assertThat(session.getPostId()).isNull();
        assertThat(session.getPostDeletedAt()).isNotNull();
        assertThat(session.getBaseXpAwarded()).isEqualTo(30.0);
        assertThat(session.getRemainingXpAwarded()).isEqualTo(70.0);
        assertThat(session.getLinkedAt()).isEqualTo(linkedAt);
        verify(sessionRepository).saveAll(org.mockito.ArgumentMatchers.<CookingSession>anyList());
    }

        @Test
        void markLinkedPostDeletedTransitionsAllPostedSessionsForSamePost() {
        LocalDateTime firstLinkedAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(3);
        LocalDateTime secondLinkedAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(1);
        CookingSession firstSession = CookingSession.builder()
            .id("session-1")
            .status(SessionStatus.POSTED)
            .postId("post-1")
            .baseXpAwarded(30.0)
            .remainingXpAwarded(70.0)
            .linkedAt(firstLinkedAt)
            .build();
        CookingSession secondSession = CookingSession.builder()
            .id("session-2")
            .status(SessionStatus.POSTED)
            .postId("post-1")
            .baseXpAwarded(25.0)
            .remainingXpAwarded(45.0)
            .linkedAt(secondLinkedAt)
            .build();

        when(sessionRepository.findAllByPostIdAndStatus("post-1", SessionStatus.POSTED))
            .thenReturn(List.of(firstSession, secondSession));

        int updated = cookingSessionService.markLinkedPostDeleted("post-1");

        assertThat(updated).isEqualTo(2);
        assertThat(firstSession.getStatus()).isEqualTo(SessionStatus.POST_DELETED);
        assertThat(secondSession.getStatus()).isEqualTo(SessionStatus.POST_DELETED);
        assertThat(firstSession.getPostId()).isNull();
        assertThat(secondSession.getPostId()).isNull();
        assertThat(firstSession.getPostDeletedAt()).isNotNull();
        assertThat(secondSession.getPostDeletedAt()).isEqualTo(firstSession.getPostDeletedAt());
        assertThat(firstSession.getLinkedAt()).isEqualTo(firstLinkedAt);
        assertThat(secondSession.getLinkedAt()).isEqualTo(secondLinkedAt);
        assertThat(firstSession.getRemainingXpAwarded()).isEqualTo(70.0);
        assertThat(secondSession.getRemainingXpAwarded()).isEqualTo(45.0);
        verify(sessionRepository).saveAll(org.mockito.ArgumentMatchers.<CookingSession>anyList());
        }

    @Test
    void markLinkedPostDeletedSkipsWhenNothingMatches() {
        when(sessionRepository.findAllByPostIdAndStatus("post-1", SessionStatus.POSTED))
                .thenReturn(List.of());

        int updated = cookingSessionService.markLinkedPostDeleted("post-1");

        assertThat(updated).isZero();
        verify(sessionRepository, never()).saveAll(org.mockito.ArgumentMatchers.<CookingSession>anyList());
    }

    @Test
    void linkSessionPublishesLinkPostXpWithRecipeContext() {
        String userId = "user-1";
        String sessionId = "session-1";
        String postId = "post-1";
        PostLinkInfo postLinkInfo = PostLinkInfo.builder()
            .postId(postId)
            .userId(userId)
            .photoCount(2)
            .build();
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId("recipe-1")
            .status(SessionStatus.COMPLETED)
            .baseXpAwarded(30.0)
            .pendingXp(70.0)
            .build();
        Recipe recipe = Recipe.builder()
            .id("recipe-1")
            .title("Spicy Noodles")
            .userId("creator-1")
            .xpReward(100)
            .rewardBadges(List.of("🔥"))
            .build();
        SessionLinkingRequest request = new SessionLinkingRequest();
        request.setPostId(postId);

        when(helper.validateSessionForLinking(sessionId, userId)).thenReturn(session);
        when(helper.validateAndGetPost(postId, userId)).thenReturn(postLinkInfo);
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(helper.calculateFinalXpForLinking(session, postLinkInfo)).thenReturn(70);
        when(helper.processCreatorBonus(recipe, userId, sessionId)).thenReturn(false);

        cookingSessionService.linkSession(userId, sessionId, request);

        verify(postProvider).updatePostXp(postId, 70.0);
        verify(helper).sendXpEventWithBadges(
            userId,
            70.0,
            "LINKING_POST",
            sessionId,
            "Linking Post ID: " + postId,
            List.of("🔥"),
            "recipe-1");
    }

    @Test
    void linkSessionPersistsDecayedPostLinkOutcomeOnSession() {
        String userId = "user-1";
        String sessionId = "session-1";
        String postId = "post-1";
        PostLinkInfo postLinkInfo = PostLinkInfo.builder()
            .postId(postId)
            .userId(userId)
            .photoCount(1)
            .build();
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId("recipe-1")
            .status(SessionStatus.COMPLETED)
            .baseXpAwarded(30.0)
            .pendingXp(70.0)
            .build();
        Recipe recipe = Recipe.builder()
            .id("recipe-1")
            .title("Spicy Noodles")
            .userId("creator-1")
            .xpReward(100)
            .rewardBadges(List.of("🔥"))
            .build();
        SessionLinkingRequest request = new SessionLinkingRequest();
        request.setPostId(postId);

        when(helper.validateSessionForLinking(sessionId, userId)).thenReturn(session);
        when(helper.validateAndGetPost(postId, userId)).thenReturn(postLinkInfo);
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(helper.calculateFinalXpForLinking(session, postLinkInfo)).thenReturn(17);
        when(helper.processCreatorBonus(recipe, userId, sessionId)).thenReturn(false);

        SessionLinkingResponse response = cookingSessionService.linkSession(userId, sessionId, request);

        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getXpAwarded()).isEqualTo(17);
        assertThat(response.getTotalXpForRecipe()).isEqualTo(47);
        assertThat(response.isCreatorBonusAwarded()).isFalse();
        assertThat(response.getBadgesEarned()).containsExactly("🔥");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.POSTED);
        assertThat(session.getPostId()).isEqualTo(postId);
        assertThat(session.getPendingXp()).isZero();
        assertThat(session.getRemainingXpAwarded()).isEqualTo(17.0);
        assertThat(session.getLinkedAt()).isNotNull();

        verify(postProvider).updatePostXp(postId, 17.0);
        verify(helper).updateRecipeStats("recipe-1", 0, 17);
        verify(sessionRepository).save(session);
    }

    @Test
    void linkSessionFailsBeforeAwardingXpWhenPostXpPersistenceFails() {
        String userId = "user-1";
        String sessionId = "session-1";
        String postId = "post-1";
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId("recipe-1")
            .status(SessionStatus.COMPLETED)
            .baseXpAwarded(30.0)
            .pendingXp(70.0)
            .build();
        Recipe recipe = Recipe.builder()
            .id("recipe-1")
            .title("Spicy Noodles")
            .userId("creator-1")
            .xpReward(100)
            .rewardBadges(List.of("🔥"))
            .build();
        SessionLinkingRequest request = new SessionLinkingRequest();
        request.setPostId(postId);

        when(helper.validateSessionForLinking(sessionId, userId)).thenReturn(session);
        when(helper.validateAndGetPost(postId, userId)).thenReturn(PostLinkInfo.builder()
            .postId(postId)
            .userId(userId)
            .photoCount(2)
            .build());
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(helper.calculateFinalXpForLinking(session, PostLinkInfo.builder()
            .postId(postId)
            .userId(userId)
            .photoCount(2)
            .build())).thenReturn(70);
        doThrow(new RuntimeException("social down"))
            .when(postProvider)
            .updatePostXp(postId, 70.0);

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.linkSession(userId, sessionId, request));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.POST_SERVICE_ERROR);
        verify(helper, never()).updateRecipeStats(any(), eq(0), eq(70));
        verify(helper, never()).sendXpEventWithBadges(any(), anyDouble(), any(), any(), any(), any(), any());
        verify(helper, never()).processCreatorBonus(any(), any(), any());
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
    }

    @Test
    void linkSessionRejectsWhenSessionAlreadyLinkedViaPostDeleted() {
        String userId = "user-1";
        String sessionId = "session-1";
        String postId = "post-2";
        SessionLinkingRequest request = new SessionLinkingRequest();
        request.setPostId(postId);

        when(helper.validateSessionForLinking(sessionId, userId))
            .thenThrow(new AppException(ErrorCode.SESSION_ALREADY_LINKED));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.linkSession(userId, sessionId, request));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.SESSION_ALREADY_LINKED);
        verify(postProvider, never()).updatePostXp(anyString(), anyDouble());
        verify(helper, never()).updateRecipeStats(anyString(), anyInt(), anyInt());
        verify(helper, never()).sendXpEventWithBadges(any(), anyDouble(), any(), any(), any(), any(), any());
        verify(helper, never()).processCreatorBonus(any(), any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void completeSessionRejectsWhenSessionIsNotInProgress() {
        String userId = "user-1";
        String sessionId = "session-1";
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId("recipe-1")
            .status(SessionStatus.PAUSED)
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.completeSession(userId, sessionId, buildCompletionRequest()));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACTION);
        verify(recipeRepository, never()).findById(anyString());
        verify(profileProvider, never()).updateAfterCompletion(any());
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
    }

    @Test
    void completeSessionRejectsWhenNoStepsWereCompleted() {
        String userId = "user-1";
        String sessionId = "session-1";
        CookingSession session = buildActiveSession(userId, sessionId, "recipe-1");
        Recipe recipe = buildRecipe("recipe-1", "Spicy Noodles", 100);
        session.setCompletedSteps(new ArrayList<>());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.completeSession(userId, sessionId, buildCompletionRequest()));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACTION);
        verify(profileProvider, never()).updateAfterCompletion(any());
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
    }

    @Test
    void completeSessionContinuesWhenRecentCookAutoDraftFailsAndSyncCarriesCookingContext() {
        String userId = "user-1";
        String sessionId = "session-1";
        CookingSession session = buildActiveSession(userId, sessionId, "recipe-1");
        Recipe recipe = buildRecipe("recipe-1", "Spicy Noodles", 100);
        ChallengeRewardResult weeklyReward = ChallengeRewardResult.builder()
            .bonusXp(10)
            .challengeTitle("Weekly Heat")
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(helper.calculateMasteryMultiplier(userId, "recipe-1")).thenReturn(1.0);
        when(challengeService.checkAndCompleteChallenge(userId, recipe)).thenReturn(Optional.empty());
        when(challengeService.checkAndCompleteWeeklyChallenge(userId, recipe)).thenReturn(Optional.of(weeklyReward));
        when(challengeService.checkAndAdvanceSeasonalChallenge(userId, recipe)).thenReturn(Optional.empty());
        when(profileProvider.isShowCookingActivity(userId)).thenReturn(true);
        when(profileProvider.getBasicProfile(userId)).thenReturn(BasicProfileInfo.builder()
            .userId(userId)
            .displayName("Chef Kix")
            .avatarUrl("/avatar.png")
            .build());
        doThrow(new RuntimeException("post service down"))
            .when(postProvider)
            .createRecentCookPost(any());
        when(profileProvider.updateAfterCompletion(any(CompletionRequest.class))).thenReturn(CompletionResult.builder()
            .userId(userId)
            .currentLevel(2)
            .currentXP(40)
            .currentXPGoal(1000)
            .completionCount(5)
            .xpToNextLevel(960)
            .build());
        when(achievementService.evaluateAfterCookingCompletion(userId, session, recipe)).thenReturn(List.of());

        SessionCompletionResponse response = cookingSessionService.completeSession(userId, sessionId, buildCompletionRequest());

        ArgumentCaptor<CompletionRequest> completionCaptor = ArgumentCaptor.forClass(CompletionRequest.class);
        verify(profileProvider).updateAfterCompletion(completionCaptor.capture());
        CompletionRequest completionRequest = completionCaptor.getValue();

        assertThat(completionRequest.getRecipeId()).isEqualTo("recipe-1");
        assertThat(completionRequest.isChallengeCompleted()).isTrue();
        assertThat(completionRequest.getXpAmount()).isEqualTo(40);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getBaseXpAwarded()).isEqualTo(40);
        assertThat(response.getPendingXp()).isEqualTo(70);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    void completeSessionFallsBackToKafkaWithRecipeAndChallengeContextWhenSyncFails() {
        String userId = "user-1";
        String sessionId = "session-1";
        CookingSession session = buildActiveSession(userId, sessionId, "recipe-1");
        Recipe recipe = buildRecipe("recipe-1", "Spicy Noodles", 100);
        ChallengeRewardResult seasonalReward = ChallengeRewardResult.builder()
            .bonusXp(20)
            .challengeTitle("Season Sprint")
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(helper.calculateMasteryMultiplier(userId, "recipe-1")).thenReturn(1.0);
        when(challengeService.checkAndCompleteChallenge(userId, recipe)).thenReturn(Optional.empty());
        when(challengeService.checkAndCompleteWeeklyChallenge(userId, recipe)).thenReturn(Optional.empty());
        when(challengeService.checkAndAdvanceSeasonalChallenge(userId, recipe)).thenReturn(Optional.of(seasonalReward));
        when(profileProvider.isShowCookingActivity(userId)).thenReturn(false);
        when(profileProvider.updateAfterCompletion(any(CompletionRequest.class)))
            .thenThrow(new RuntimeException("identity down"));
        when(achievementService.evaluateAfterCookingCompletion(userId, session, recipe)).thenReturn(List.of());

        SessionCompletionResponse response = cookingSessionService.completeSession(userId, sessionId, buildCompletionRequest());

        verify(helper).sendXpEventWithChallenge(
            eq(userId),
            eq(50.0),
            eq("COOKING_SESSION"),
            eq(sessionId),
            contains("Season Sprint"),
            eq(true),
            eq("recipe-1"));
        assertThat(response.getBaseXpAwarded()).isEqualTo(50);
        assertThat(response.getPendingXp()).isEqualTo(70);
        assertThat(response.getCurrentXp()).isNull();
    }

    @Test
    void pauseSessionRejectsWhenActiveTimersAreRunning() {
        String userId = "user-1";
        String sessionId = "session-1";
        setAuthenticatedUser(userId);

        CookingSession.ActiveTimer activeTimer = CookingSession.ActiveTimer.builder()
            .stepNumber(2)
            .totalSeconds(600)
            .remainingSeconds(420)
            .startedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(3))
            .build();
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.IN_PROGRESS)
            .activeTimers(new ArrayList<>(List.of(activeTimer)))
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.pauseSession(sessionId));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.CANNOT_PAUSE_WITH_ACTIVE_TIMERS);
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
        verify(activeCookingRepository, never()).removeActive(userId);
    }

    @Test
    void pauseSessionMarksPausedAndSetsThreeHourResumeDeadline() {
        String userId = "user-1";
        String sessionId = "session-1";
        setAuthenticatedUser(userId);

        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.IN_PROGRESS)
            .activeTimers(new ArrayList<>())
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        cookingSessionService.pauseSession(sessionId);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.PAUSED);
        assertThat(session.getPausedAt()).isNotNull();
        assertThat(session.getResumeDeadline()).isEqualTo(session.getPausedAt().plusHours(3));
        verify(sessionRepository).save(session);
        verify(activeCookingRepository).removeActive(userId);
    }

    @Test
    void pauseSessionRejectsWhenSessionIsNotInProgress() {
        String userId = "user-1";
        String sessionId = "session-1";
        setAuthenticatedUser(userId);

        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.PAUSED)
            .activeTimers(new ArrayList<>())
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.pauseSession(sessionId));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACTION);
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
        verify(activeCookingRepository, never()).removeActive(userId);
    }

    @Test
    void pauseSessionAllowsNullActiveTimersAndStillSetsPauseState() {
        String userId = "user-1";
        String sessionId = "session-1";
        setAuthenticatedUser(userId);

        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.IN_PROGRESS)
            .activeTimers(null)
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        cookingSessionService.pauseSession(sessionId);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.PAUSED);
        assertThat(session.getPausedAt()).isNotNull();
        assertThat(session.getResumeDeadline()).isEqualTo(session.getPausedAt().plusHours(3));
        verify(sessionRepository).save(session);
        verify(activeCookingRepository).removeActive(userId);
    }

    @Test
    void resumeSessionClearsPauseMetadataAndRestoresPresence() {
        String userId = "user-1";
        String sessionId = "session-1";
        String recipeId = "recipe-1";
        setAuthenticatedUser(userId);

        LocalDateTime pausedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20);
        LocalDateTime resumeDeadline = pausedAt.plusHours(3);
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId(recipeId)
            .recipeTitle("Spicy Noodles")
            .coverImageUrl(List.of("/cover.jpg"))
            .startedAt(pausedAt.minusMinutes(12))
            .currentStep(3)
            .status(SessionStatus.PAUSED)
            .pausedAt(pausedAt)
            .resumeDeadline(resumeDeadline)
            .build();
        Recipe recipe = Recipe.builder()
            .id(recipeId)
            .steps(List.of())
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(profileProvider.getBasicProfile(userId)).thenReturn(BasicProfileInfo.builder()
            .userId(userId)
            .username("chefkix")
            .displayName("Chef Kix")
            .avatarUrl("/avatar.png")
            .build());

        cookingSessionService.resumeSession(sessionId);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getPausedAt()).isNull();
        assertThat(session.getResumeDeadline()).isNull();
        verify(sessionRepository).save(session);
        verify(activeCookingRepository).setActive(any());
    }

    @Test
    void resumeSessionRejectsWhenSessionIsNotPaused() {
        String userId = "user-1";
        String sessionId = "session-1";
        setAuthenticatedUser(userId);

        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.IN_PROGRESS)
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.resumeSession(sessionId));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACTION);
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
        verify(activeCookingRepository, never()).setActive(any());
    }

    @Test
    void resumeSessionAllowsNullResumeDeadlineAndStillRestoresPresence() {
        String userId = "user-1";
        String sessionId = "session-1";
        String recipeId = "recipe-1";
        setAuthenticatedUser(userId);

        LocalDateTime pausedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20);
        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId(recipeId)
            .recipeTitle("Spicy Noodles")
            .coverImageUrl(List.of("/cover.jpg"))
            .startedAt(pausedAt.minusMinutes(12))
            .currentStep(3)
            .status(SessionStatus.PAUSED)
            .pausedAt(pausedAt)
            .resumeDeadline(null)
            .build();
        Recipe recipe = Recipe.builder()
            .id(recipeId)
            .steps(List.of())
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(profileProvider.getBasicProfile(userId)).thenReturn(BasicProfileInfo.builder()
            .userId(userId)
            .username("chefkix")
            .displayName("Chef Kix")
            .avatarUrl("/avatar.png")
            .build());

        cookingSessionService.resumeSession(sessionId);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getPausedAt()).isNull();
        assertThat(session.getResumeDeadline()).isNull();
        verify(sessionRepository).save(session);
        verify(activeCookingRepository).setActive(any());
    }

    @Test
    void resumeSessionRejectsExpiredPauseDeadline() {
        String userId = "user-1";
        String sessionId = "session-1";
        setAuthenticatedUser(userId);

        CookingSession session = CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.PAUSED)
            .pausedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(4))
            .resumeDeadline(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        AppException thrown = assertThrows(
            AppException.class,
            () -> cookingSessionService.resumeSession(sessionId));

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.SESSION_EXPIRED);
        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.same(session));
        verify(activeCookingRepository, never()).setActive(any());
    }

    private void setAuthenticatedUser(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userId, null));
    }

    private CookingSession buildActiveSession(String userId, String sessionId, String recipeId) {
        return CookingSession.builder()
            .id(sessionId)
            .userId(userId)
            .recipeId(recipeId)
            .status(SessionStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(42))
            .completedSteps(new ArrayList<>(List.of(1)))
            .build();
    }

    private Recipe buildRecipe(String recipeId, String title, int xpReward) {
        return Recipe.builder()
            .id(recipeId)
            .title(title)
            .xpReward(xpReward)
            .coverImageUrl(List.of("/cover.jpg"))
            .rewardBadges(List.of())
            .steps(List.of())
            .build();
    }

    private CompleteSessionRequest buildCompletionRequest() {
        CompleteSessionRequest request = new CompleteSessionRequest();
        request.setRating(5);
        request.setNotes("Completed cleanly");
        return request;
    }
}