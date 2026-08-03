package com.chefkix.culinary.features.session.service;

import com.chefkix.social.api.PostProvider;
import com.chefkix.social.api.dto.RecentCookRequest;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeRewardResult;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.model.ActiveCookingPresence;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.enums.TimerEventType;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.features.session.dto.request.*;
import com.chefkix.culinary.features.session.dto.response.*;
import com.chefkix.culinary.features.session.mapper.CookingSessionMapper;
import com.chefkix.culinary.features.session.repository.ActiveCookingRedisRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.challenge.service.ChallengeService;
import com.chefkix.culinary.features.duel.service.DuelService;
import com.chefkix.culinary.features.room.model.CookingRoom;
import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CookingSessionService {

    private static final String SHARE_URL_BASE = "https://chefkix.app/recipes/";

    private final CookingSessionRepository sessionRepository;
    private final RecipeRepository recipeRepository;
    private final CookingSessionMapper sessionMapper;
    private final ChallengeService challengeService;
    private final RecipeHelper helper;
    private final ProfileProvider profileProvider;
    private final PostProvider postProvider;
    private final CookingRoomRedisRepository roomRepository;
    private final ActiveCookingRedisRepository activeCookingRepository;
    private final com.chefkix.culinary.features.achievement.service.AchievementService achievementService;
    private final DuelService duelService;

    @Transactional
    public StartSessionResponse startSession(String userId, StartSessionRequest request) {
        Optional<CookingSession> activeSessionOpt = sessionRepository
                .findFirstByUserIdAndStatusIn(userId, List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED));

        if (activeSessionOpt.isPresent()) {
            throw new AppException(ErrorCode.SESSION_ALREADY_ACTIVE);
        }

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (recipe.getStatus() != RecipeStatus.PUBLISHED && !userId.equals(recipe.getUserId())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }

        CookingSession session = CookingSession.builder()
                .userId(userId)
                .recipeId(request.getRecipeId())
                .recipeTitle(recipe.getTitle())
                .coverImageUrl(recipe.getCoverImageUrl())
                .status(SessionStatus.IN_PROGRESS)
            .startedAt(utcNow())
                .currentStep(1)
                .completedSteps(new ArrayList<>())
                .activeTimers(new ArrayList<>())
                .flagged(false)
                .build();

        sessionRepository.save(session);
        setActiveCookingPresence(userId, session, recipe);
        return sessionMapper.toStartSessionResponse(session, recipe);
    }

    @Transactional
    public void logTimerEvent(String userId, String sessionId, TimerEventRequest request) {
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        TimerEventType eventType = helper.validateAndParseTimerEvent(userId, session, request);
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        LocalDateTime serverNow = utcNow();

        CookingSession.TimerEvent logEvent = CookingSession.TimerEvent.builder()
                .stepNumber(request.getStepNumber())
                .event(eventType)
                .clientTimestamp(request.getClientTimestamp())
                .serverTimestamp(serverNow)
                .build();

        if (session.getTimerEvents() == null) session.setTimerEvents(new ArrayList<>());
        session.getTimerEvents().add(logEvent);

        switch (eventType) {
            case START -> helper.handleTimerStart(session, recipe, request.getStepNumber(), serverNow);
            case COMPLETE, SKIP -> helper.handleTimerStop(session, request.getStepNumber());
        }

        sessionRepository.save(session);
    }

    @Transactional
    public SessionCompletionResponse completeSession(String userId, String sessionId, CompleteSessionRequest request) {
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) throw new AppException(ErrorCode.INVALID_ACTION);

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        helper.validateAntiCheat(session, recipe);

        if (session.getCompletedSteps() == null || session.getCompletedSteps().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        double masteryMult = helper.calculateMasteryMultiplier(userId, session.getRecipeId());
        double totalEffectiveXp = recipe.getXpReward() * masteryMult;
        double recipeImmediateXp = Math.round(totalEffectiveXp * 0.30);
        double pendingXp = Math.round(totalEffectiveXp * 0.70);

        Optional<ChallengeRewardResult> challengeResult = challengeService.checkAndCompleteChallenge(userId, recipe);
        Optional<ChallengeRewardResult> weeklyResult = challengeService.checkAndCompleteWeeklyChallenge(userId, recipe);

        try {
            challengeService.checkAndAdvanceCommunityChallenge(userId, recipe);
        } catch (Exception e) {
            log.warn("Community challenge check failed for user {}: {}", userId, e.getMessage());
        }

        Optional<ChallengeRewardResult> seasonalResult = challengeService.checkAndAdvanceSeasonalChallenge(userId, recipe);
        List<ChallengeRewardResult> completedChallengeRewards = java.util.stream.Stream.of(
                        challengeResult, weeklyResult, seasonalResult)
                .flatMap(Optional::stream)
                .toList();
        int challengeBonusXp = completedChallengeRewards.stream()
                .mapToInt(ChallengeRewardResult::getBonusXp)
                .sum();
        double immediateXpBeforeCoOp = recipeImmediateXp + challengeBonusXp;
        double baseXp = immediateXpBeforeCoOp;
        boolean challengeCompleted =
                !completedChallengeRewards.isEmpty();

        double coOpMultiplier = 1.0;
        String coOpReason = null;
        if (session.getRoomCode() != null) {
            var roomOpt = roomRepository.findByRoomCode(session.getRoomCode());
            if (roomOpt.isPresent()) {
                CookingRoom room = roomOpt.get();
                long cookCount = room.getParticipants().stream()
                        .filter(p -> !"SPECTATOR".equals(p.getRole()))
                        .count();
                if (cookCount == 2) { coOpMultiplier = 1.2; coOpReason = "CO_OP_DUO"; }
                else if (cookCount >= 3) { coOpMultiplier = 1.1; coOpReason = "CO_OP_GROUP"; }
            }
        }
        if (coOpMultiplier > 1.0) {
            baseXp = Math.round(baseXp * coOpMultiplier);
            pendingXp = Math.round(pendingXp * coOpMultiplier);
            log.info("Co-op multiplier applied: {}× ({}) for session {}", coOpMultiplier, coOpReason, sessionId);
        }
        int coOpBonusXp = (int) Math.round(baseXp - immediateXpBeforeCoOp);

        LocalDateTime now = utcNow();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(now);
        session.setPostDeadline(now.plusDays(14));
        session.setBaseXpAwarded(baseXp);
        session.setPendingXp(pendingXp);
        session.setXpMultiplier(coOpMultiplier > 1.0 ? coOpMultiplier : null);
        session.setXpMultiplierReason(coOpReason);
        session.setRating(request.getRating());
        session.setNotes(request.getNotes());
        sessionRepository.save(session);
        removeActiveCookingPresence(userId);

        helper.updateRecipeStats(recipe.getId(), 1, 0);

        try {
            if (profileProvider.isShowCookingActivity(userId)) {
                BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
                if (profile != null) {
                    int durationMinutes = (int) java.time.Duration.between(session.getStartedAt(), now).toMinutes();
                    postProvider.createRecentCookPost(
                            RecentCookRequest.builder()
                                    .userId(userId)
                                    .sessionId(sessionId)
                                    .recipeId(recipe.getId())
                                    .recipeTitle(recipe.getTitle())
                                    .coverImageUrl(recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty()
                                            ? recipe.getCoverImageUrl().get(0) : null)
                                    .durationMinutes(durationMinutes)
                                    .displayName(profile.getDisplayName())
                                    .avatarUrl(profile.getAvatarUrl())
                                    .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to auto-create RECENT_COOK post for session {}: {}", sessionId, e.getMessage());
        }

        String challengeTitles = String.join(" & ", completedChallengeRewards.stream()
                .map(ChallengeRewardResult::getChallengeTitle)
                .toList());
        String description = "Completed cooking: " + recipe.getTitle()
                + (!challengeTitles.isBlank() ? " & Challenge: " + challengeTitles : "");
        String idempotencyKey = "xp:COOKING_SESSION:" + userId + ":" + sessionId;
        CompletionRequest completionRequest = CompletionRequest.builder()
                .userId(userId)
                .xpAmount((int) baseXp)
                .sessionId(sessionId)
            .recipeId(recipe.getId())
            .challengeCompleted(challengeCompleted)
.newBadges(null)
                .idempotencyKey(idempotencyKey)
                .build();

        CompletionResult profileResult = null;
        String xpDeliveryStatus = "QUEUED";
        try {
            profileResult = profileProvider.updateAfterCompletion(completionRequest);
            if (profileResult != null) {
                xpDeliveryStatus = "APPLIED";
                log.info("Completion XP applied for user {}: +{} XP, leveledUp={}, level {}->{}",
                        userId, baseXp, profileResult.isLeveledUp(), profileResult.getOldLevel(), profileResult.getNewLevel());
            } else {
                throw new IllegalStateException("Identity completion returned no result");
            }
        } catch (Exception e) {
            log.error("Failed to sync XP with identity service for user {}: {}", userId, e.getMessage());
            helper.sendXpEventWithChallenge(
                    userId,
                    baseXp,
                    "COOKING_SESSION",
                    sessionId,
                    description,
                    challengeCompleted,
                    recipe.getId());
        }

        List<String> newAchievements = List.of();
        try {
            newAchievements = achievementService.evaluateAfterCookingCompletion(userId, session, recipe);
        } catch (Exception e) {
            log.warn("Achievement evaluation failed for user {}: {}", userId, e.getMessage());
        }

        try {
            duelService.onSessionCompleted(userId, session);
        } catch (Exception e) {
            log.warn("Duel linkage failed for user {}: {}", userId, e.getMessage());
        }

        int baseXpInt = (int) Math.round(baseXp);
        int pendingXpInt = (int) Math.round(pendingXp);
        String completionMessage = "APPLIED".equals(xpDeliveryStatus)
                ? "Congrats! +" + baseXpInt + " XP earned. Post to unlock " + pendingXpInt + " more XP!"
                : "Cooking complete. +" + baseXpInt + " XP is processing. Post to unlock " + pendingXpInt + " more XP!";
        SessionCompletionResponse.SessionCompletionResponseBuilder responseBuilder = SessionCompletionResponse.builder()
                .sessionId(session.getId())
                .status("COMPLETED")
                .baseXpAwarded(baseXpInt)
                .recipeXpAwarded((int) Math.round(recipeImmediateXp))
                .coOpBonusXp(coOpBonusXp)
                .pendingXp(pendingXpInt)
                .xpBreakdown(recipe.getXpBreakdown())
                .completedChallengeRewards(completedChallengeRewards)
                .xpDeliveryStatus(xpDeliveryStatus)
                .postDeadline(session.getPostDeadline())
                .xpMultiplier(coOpMultiplier > 1.0 ? coOpMultiplier : null)
                .xpMultiplierReason(coOpReason)
                .newAchievements(newAchievements)
                .message(completionMessage);

        if (profileResult != null) {
            responseBuilder
                    .leveledUp(profileResult.isLeveledUp())
                    .oldLevel(profileResult.getOldLevel())
                    .newLevel(profileResult.getNewLevel())
                    .currentXp(profileResult.getCurrentXP())
                    .xpToNextLevel(profileResult.getXpToNextLevel());
        }

        return responseBuilder.build();
    }

    @Transactional
    public SessionLinkingResponse linkSession(String userId, String sessionId, SessionLinkingRequest request) {
        CookingSession session = helper.validateSessionForLinking(sessionId, userId);
        PostLinkInfo postData = helper.validateAndGetPost(request.getPostId(), userId);
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        int finalXpToAward = helper.calculateFinalXpForLinking(session, postData);

        List<String> badgesEarned = (recipe.getRewardBadges() != null) 
                ? new ArrayList<>(recipe.getRewardBadges()) 
                : new ArrayList<>();

        try {
            postProvider.updatePostXp(request.getPostId(), finalXpToAward);
            log.info("Updated post {} with xpEarned={}", request.getPostId(), finalXpToAward);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to persist post xpEarned for post {}: {}", request.getPostId(), e.getMessage());
            throw new AppException(ErrorCode.POST_SERVICE_ERROR);
        }

        helper.updateRecipeStats(recipe.getId(), 0, finalXpToAward);
        helper.sendXpEventWithBadges(
            userId,
            finalXpToAward,
            "LINKING_POST",
            sessionId,
            "Linking Post ID: " + request.getPostId(),
            badgesEarned,
            recipe.getId());

        boolean creatorBonusAwarded = helper.processCreatorBonus(recipe, userId, sessionId);

        session.setStatus(SessionStatus.POSTED);
        session.setPostId(request.getPostId());
        session.setPendingXp(0.0);
        session.setRemainingXpAwarded((double) finalXpToAward);
        session.setLinkedAt(utcNow());
        sessionRepository.save(session);

        return SessionLinkingResponse.builder()
                .sessionId(session.getId())
                .postId(request.getPostId())
                .badgesEarned(badgesEarned)
                .xpAwarded(finalXpToAward)
                .totalXpForRecipe((int) Math.round((session.getBaseXpAwarded() != null ? session.getBaseXpAwarded() : 0) + finalXpToAward))
                .creatorBonusAwarded(creatorBonusAwarded)
                .build();
    }

    public CurrentSessionResponse getCurrentSession() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<CookingSession> sessionOpt = sessionRepository
                .findFirstByUserIdAndStatusIn(userId, List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED));

        if (sessionOpt.isEmpty()) return null;

        CookingSession session = sessionOpt.get();
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        helper.calculateRemainingTime(session);
        return helper.mapToCurrentSessionResponse(session, recipe);
    }

    public CurrentSessionResponse getBySessionId(String sessionId, String userId) {
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) throw new AppException(ErrorCode.UNAUTHORIZED);

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        helper.calculateRemainingTime(session);
        return helper.mapToCurrentSessionResponse(session, recipe);
    }

    public Page<SessionHistoryResponse.SessionItemDto> getSessionHistory(String userId, SessionHistoryQuery dto, Pageable pageable) {
        return sessionRepository.findSessionHistory(userId, dto, pageable)
                .map(sessionMapper::toSessionItemDto);
    }

    public List<SessionHistoryResponse.SessionItemDto> getPendingSessions(String userId) {
        return sessionRepository
                .findTop20ByUserIdAndStatusAndPostIdIsNullOrderByCompletedAtDesc(userId, SessionStatus.COMPLETED)
                .stream()
                .map(sessionMapper::toSessionItemDto)
                .toList();
    }

    @Transactional
    public int markLinkedPostDeleted(String postId) {
        if (postId == null || postId.isBlank()) {
            return 0;
        }

        List<CookingSession> linkedSessions = sessionRepository.findAllByPostIdAndStatus(postId, SessionStatus.POSTED);
        if (linkedSessions.isEmpty()) {
            return 0;
        }

        LocalDateTime deletedAt = utcNow();
        linkedSessions.forEach(session -> {
            session.setStatus(SessionStatus.POST_DELETED);
            session.setPostDeletedAt(deletedAt);
            session.setPostId(null);
        });
        sessionRepository.saveAll(linkedSessions);
        log.info("Marked {} cooking sessions as POST_DELETED for removed post {}", linkedSessions.size(), postId);
        return linkedSessions.size();
    }

    public SessionNavigateResponse getSessionCurrentStep(String sessionId, SessionNavigateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        int totalSteps = recipe.getSteps().size();
        int currentDbStep = session.getCurrentStep();
        int newStep = currentDbStep;

        switch (request.getAction().toLowerCase()) {
            case "next" -> newStep = (currentDbStep < totalSteps) ? currentDbStep + 1 : currentDbStep;
            case "previous" -> newStep = (currentDbStep > 1) ? currentDbStep - 1 : currentDbStep;
            case "goto" -> {
                Integer target = request.getTargetStep();
                if (target == null || target < 1 || target > totalSteps) throw new AppException(ErrorCode.INVALID_TARGET_STEP);
                newStep = target;
            }
            default -> throw new AppException(ErrorCode.INVALID_NAVIGATION_ACTION);
        }

        if (newStep != currentDbStep) {
            session.setCurrentStep(newStep);
            sessionRepository.save(session);
        }

        helper.calculateRemainingTime(session);
        return SessionNavigateResponse.builder()
                .sessionId(session.getId())
                .currentStep(newStep)
                .previousStep((newStep == 1) ? 1 : newStep - 1)
                .activeTimers(session.getActiveTimers())
                .build();
    }

    @Transactional
    public SessionPauseResponse pauseSession(String sessionId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        if (session.getActiveTimers() != null && !session.getActiveTimers().isEmpty()) {
            throw new AppException(ErrorCode.CANNOT_PAUSE_WITH_ACTIVE_TIMERS);
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) throw new AppException(ErrorCode.INVALID_ACTION);

        LocalDateTime now = utcNow();
        LocalDateTime deadline = now.plusHours(3);

        session.setStatus(SessionStatus.PAUSED);
        session.setPausedAt(now);
        session.setResumeDeadline(deadline);
        sessionRepository.save(session);
        removeActiveCookingPresence(userId);

        return SessionPauseResponse.builder()
                .sessionId(session.getId())
                .status(SessionStatus.PAUSED)
                .pauseAt(session.getPausedAt())
                .resumeDeadline(deadline)
                .build();
    }

    @Transactional
    public SessionResumeResponse resumeSession(String sessionId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        if (session.getResumeDeadline() != null && utcNow().isAfter(session.getResumeDeadline())) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setPausedAt(null);
        session.setResumeDeadline(null);
        sessionRepository.save(session);

        Recipe recipe = recipeRepository.findById(session.getRecipeId()).orElse(null);
        setActiveCookingPresence(userId, session, recipe);

        return SessionResumeResponse.builder()
                .sessionId(session.getId())
                .status(SessionStatus.IN_PROGRESS)
                .resumeAt(utcNow())
                .build();
    }

    /**
     */
    @Transactional
    public CompleteStepResponse completeStep(String sessionId, CompleteStepRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        int totalSteps = recipe.getSteps().size();
        int stepNumber = request.getStepNumber();

        if (stepNumber < 1 || stepNumber > totalSteps) {
            throw new AppException(ErrorCode.INVALID_TARGET_STEP);
        }

        if (session.getCompletedSteps() == null) {
            session.setCompletedSteps(new ArrayList<>());
        }

        boolean alreadyCompleted = session.getCompletedSteps().contains(stepNumber);
        
        if (!alreadyCompleted) {
            session.getCompletedSteps().add(stepNumber);
            sessionRepository.save(session);
            log.info("Step {} completed for session {} by user {}", stepNumber, sessionId, userId);
        }

        boolean allStepsComplete = session.getCompletedSteps().size() == totalSteps;

        return CompleteStepResponse.builder()
                .sessionId(sessionId)
                .completedStep(stepNumber)
                .completedSteps(session.getCompletedSteps())
                .totalSteps(totalSteps)
                .allStepsComplete(allStepsComplete)
                .alreadyCompleted(alreadyCompleted)
                .build();
    }

    /**
     */
    @Transactional
    public SessionAbandonResponse abandonActiveSession() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<CookingSession> sessionOpt = sessionRepository
                .findFirstByUserIdAndStatusIn(userId, List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED));

        if (sessionOpt.isEmpty()) {
            return SessionAbandonResponse.builder()
                    .sessionId(null)
                    .status("none")
                    .abandonedAt(null)
                    .abandoned(false)
                    .build();
        }

        return abandonSession(sessionOpt.get().getId());
    }

    /**
     */
    @Transactional
    public SessionAbandonResponse abandonSession(String sessionId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus().hasClaimedPostXp()) {
            throw new AppException(ErrorCode.SESSION_COMPLETED);
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            return SessionAbandonResponse.builder()
                    .sessionId(sessionId)
                    .status(SessionStatus.ABANDONED.getValue())
                    .abandonedAt(utcNow())
                    .abandoned(true)
                    .build();
        }

        LocalDateTime now = utcNow();
        session.setStatus(SessionStatus.ABANDONED);
        session.setAbandonedAt(now);
        sessionRepository.save(session);
        removeActiveCookingPresence(userId);

        log.info("Session {} abandoned by user {}", sessionId, userId);

        return SessionAbandonResponse.builder()
                .sessionId(sessionId)
                .status(SessionStatus.ABANDONED.getValue())
                .abandonedAt(now)
                .abandoned(true)
                .build();
    }

    public CookingSession getSessionById(String sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }


    /**
     */
    public FriendCookingActivityResponse getFriendsActiveCooking() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<String> followingIds = profileProvider.getFollowingIds(userId);
        if (followingIds == null || followingIds.isEmpty()) {
            return FriendCookingActivityResponse.builder()
                    .friends(List.of())
                    .totalActive(0)
                    .build();
        }

        List<ActiveCookingPresence> activePresences = activeCookingRepository.getActiveForUsers(followingIds);

        List<FriendCookingActivityResponse.ActiveFriend> friends = activePresences.stream()
                .map(p -> FriendCookingActivityResponse.ActiveFriend.builder()
                        .userId(p.getUserId())
                        .username(p.getUsername())
                        .displayName(p.getDisplayName())
                        .avatarUrl(p.getAvatarUrl())
                        .recipeId(p.getRecipeId())
                        .recipeTitle(p.getRecipeTitle())
                        .coverImageUrl(p.getCoverImageUrl())
                        .currentStep(p.getCurrentStep())
                        .totalSteps(p.getTotalSteps())
                        .startedAt(p.getStartedAt())
                        .roomCode(p.getRoomCode())
                        .build())
                .toList();

        return FriendCookingActivityResponse.builder()
                .friends(friends)
                .totalActive(friends.size())
                .build();
    }


    /**
     */
    private void setActiveCookingPresence(String userId, CookingSession session, Recipe recipe) {
        try {
            BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
            ActiveCookingPresence presence = ActiveCookingPresence.builder()
                    .userId(userId)
                    .username(profile != null ? profile.getUsername() : null)
                    .displayName(profile != null ? profile.getDisplayName() : null)
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .recipeId(session.getRecipeId())
                    .recipeTitle(session.getRecipeTitle())
                    .coverImageUrl(session.getCoverImageUrl())
                    .currentStep(session.getCurrentStep() != null ? session.getCurrentStep() : 1)
                    .totalSteps(recipe != null && recipe.getSteps() != null ? recipe.getSteps().size() : 0)
                    .startedAt(session.getStartedAt())
                    .roomCode(session.getRoomCode())
                    .build();
            activeCookingRepository.setActive(presence);
        } catch (Exception e) {
            log.warn("Failed to set cooking presence for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     */
    private void removeActiveCookingPresence(String userId) {
        try {
            activeCookingRepository.removeActive(userId);
        } catch (Exception e) {
            log.warn("Failed to remove cooking presence for user {}: {}", userId, e.getMessage());
        }
    }


    /**
     */
    public CookCardDataResponse getCookCardData(String sessionId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        if (session.getStatus() == null || !session.getStatus().countsAsCompletedCook()) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        String difficulty = null;
        Integer totalSteps = null;
        Recipe recipe = recipeRepository.findById(session.getRecipeId()).orElse(null);
        if (recipe != null) {
            difficulty = recipe.getDifficulty() != null ? recipe.getDifficulty().getValue() : null;
            totalSteps = recipe.getSteps() != null ? recipe.getSteps().size() : null;
        }

        String displayName = null;
        String avatarUrl = null;
        try {
            BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
            if (profile != null) {
                displayName = profile.getDisplayName();
                avatarUrl = profile.getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("Could not fetch profile for cook card: userId={}", userId);
        }

        Long cookingTimeMinutes = null;
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            cookingTimeMinutes = java.time.Duration.between(session.getStartedAt(), session.getCompletedAt()).toMinutes();
        }

        int xpEarned = 0;
        if (session.getBaseXpAwarded() != null) {
            xpEarned += session.getBaseXpAwarded().intValue();
        }
        if (session.getRemainingXpAwarded() != null) {
            xpEarned += session.getRemainingXpAwarded().intValue();
        }

        String shareUrl = SHARE_URL_BASE + session.getRecipeId();

        return CookCardDataResponse.builder()
                .sessionId(session.getId())
                .completedAt(session.getCompletedAt())
                .xpEarned(xpEarned)
                .stepsCompleted(session.getCompletedSteps() != null ? session.getCompletedSteps().size() : 0)
                .totalSteps(totalSteps)
                .cookingTimeMinutes(cookingTimeMinutes)
                .rating(session.getRating())
                .recipeId(session.getRecipeId())
                .recipeTitle(session.getRecipeTitle())
                .coverImageUrl(session.getCoverImageUrl())
                .difficulty(difficulty)
                .userId(userId)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .shareUrl(shareUrl)
                .build();
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
