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
        // 1. Check existing session
        Optional<CookingSession> activeSessionOpt = sessionRepository
                .findFirstByUserIdAndStatusIn(userId, List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED));

        if (activeSessionOpt.isPresent()) {
            throw new AppException(ErrorCode.SESSION_ALREADY_ACTIVE);
        }

        // 2. Fetch and validate recipe
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // Block cooking unpublished/archived/draft recipes (unless owner)
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

        // Only allow timer events on active sessions
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        // 1. Validate & Parse Event via Helper
        TimerEventType eventType = helper.validateAndParseTimerEvent(userId, session, request);
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        LocalDateTime serverNow = utcNow();

        // 2. Audit Log
        CookingSession.TimerEvent logEvent = CookingSession.TimerEvent.builder()
                .stepNumber(request.getStepNumber())
                .event(eventType)
                .clientTimestamp(request.getClientTimestamp())
                .serverTimestamp(serverNow)
                .build();

        if (session.getTimerEvents() == null) session.setTimerEvents(new ArrayList<>());
        session.getTimerEvents().add(logEvent);

        // 3. Delegate Timer handling logic to Helper
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

        // 1. Anti-cheat check
        helper.validateAntiCheat(session, recipe);

        // 1b. Require at least 1 completed step to prevent zero-effort XP farming
        if (session.getCompletedSteps() == null || session.getCompletedSteps().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        // 2. Calculate Mastery & Base XP
        double masteryMult = helper.calculateMasteryMultiplier(userId, session.getRecipeId());
        double totalEffectiveXp = recipe.getXpReward() * masteryMult;
        // Round to integers immediately to avoid floating point precision errors (e.g., 125.99999999999999)
        double baseXp = Math.round(totalEffectiveXp * 0.30);
        double pendingXp = Math.round(totalEffectiveXp * 0.70);

        // 3. Check Challenge (Daily)
        Optional<ChallengeRewardResult> challengeResult = challengeService.checkAndCompleteChallenge(userId, recipe);
        String challengeTitle = null;
        if (challengeResult.isPresent()) {
            baseXp += challengeResult.get().getBonusXp();
            challengeTitle = challengeResult.get().getChallengeTitle();
        }

        // 3b. Check Weekly Challenge
        Optional<ChallengeRewardResult> weeklyResult = challengeService.checkAndCompleteWeeklyChallenge(userId, recipe);
        if (weeklyResult.isPresent()) {
            baseXp += weeklyResult.get().getBonusXp();
            if (challengeTitle == null) {
                challengeTitle = weeklyResult.get().getChallengeTitle();
            } else {
                challengeTitle += " & " + weeklyResult.get().getChallengeTitle();
            }
        }

        // 3c. Check Community Challenge (global progress, fire-and-forget)
        try {
            challengeService.checkAndAdvanceCommunityChallenge(userId, recipe);
        } catch (Exception e) {
            log.warn("Community challenge check failed for user {}: {}", userId, e.getMessage());
        }

        // 3d. Check Seasonal Challenge
        Optional<ChallengeRewardResult> seasonalResult = challengeService.checkAndAdvanceSeasonalChallenge(userId, recipe);
        if (seasonalResult.isPresent()) {
            baseXp += seasonalResult.get().getBonusXp();
            if (challengeTitle == null) {
                challengeTitle = seasonalResult.get().getChallengeTitle();
            } else {
                challengeTitle += " & " + seasonalResult.get().getChallengeTitle();
            }
        }
        boolean challengeCompleted =
                challengeResult.isPresent() || weeklyResult.isPresent() || seasonalResult.isPresent();

        // 3e. Co-op XP multiplier (from co-cooking rooms)
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

        // 4. Update Session Status
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

        // 5. Update Stats
        helper.updateRecipeStats(recipe.getId(), 1, 0);

        // 5b. Auto-draft RECENT_COOK post (if user has showCookingActivity enabled)
        try {
            if (profileProvider.isShowCookingActivity(userId)) {
                BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
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
        } catch (Exception e) {
            log.warn("Failed to auto-create RECENT_COOK post for session {}: {}", sessionId, e.getMessage());
        }

        // 6. Sync call to identity service for XP + level-up detection
        String description = "Completed cooking: " + recipe.getTitle() + (challengeTitle != null ? " & Challenge: " + challengeTitle : "");
        // Deterministic idempotency key: same key used by both sync and Kafka paths
        // so that if sync succeeds, the Kafka fallback event is deduped in Redis.
        String idempotencyKey = "xp:COOKING_SESSION:" + userId + ":" + sessionId;
        CompletionRequest completionRequest = CompletionRequest.builder()
                .userId(userId)
                .xpAmount((int) baseXp)
            .recipeId(recipe.getId())
            .challengeCompleted(challengeCompleted)
                .newBadges(null) // Badges only on post, not on complete
                .idempotencyKey(idempotencyKey)
                .build();

        CompletionResult profileResult = null;
        try {
            profileResult = profileProvider.updateAfterCompletion(completionRequest);
            if (profileResult != null) {
                log.info("Completion XP applied for user {}: +{} XP, leveledUp={}, level {}->{}",
                        userId, baseXp, profileResult.isLeveledUp(), profileResult.getOldLevel(), profileResult.getNewLevel());
            }
        } catch (Exception e) {
            log.error("Failed to sync XP with identity service for user {}: {}", userId, e.getMessage());
            // Fallback: send via Kafka for reliability
            helper.sendXpEventWithChallenge(
                    userId,
                    baseXp,
                    "COOKING_SESSION",
                    sessionId,
                    description,
                    challengeCompleted,
                    recipe.getId());
        }

        // 7. Achievement evaluation (fire-and-forget — never blocks completion)
        List<String> newAchievements = List.of();
        try {
            newAchievements = achievementService.evaluateAfterCookingCompletion(userId, session, recipe);
        } catch (Exception e) {
            log.warn("Achievement evaluation failed for user {}: {}", userId, e.getMessage());
        }

        // 8. Duel linkage (fire-and-forget — never blocks completion)
        try {
            duelService.onSessionCompleted(userId, session);
        } catch (Exception e) {
            log.warn("Duel linkage failed for user {}: {}", userId, e.getMessage());
        }

        // 9. Build response with level-up info (round to integers for clean XP values)
        int baseXpInt = (int) Math.round(baseXp);
        int pendingXpInt = (int) Math.round(pendingXp);
        SessionCompletionResponse.SessionCompletionResponseBuilder responseBuilder = SessionCompletionResponse.builder()
                .sessionId(session.getId())
                .status("COMPLETED")
                .baseXpAwarded(baseXpInt)
                .pendingXp(pendingXpInt)
                .xpBreakdown(recipe.getXpBreakdown())
                .postDeadline(session.getPostDeadline())
                .xpMultiplier(coOpMultiplier > 1.0 ? coOpMultiplier : null)
                .xpMultiplierReason(coOpReason)
                .newAchievements(newAchievements)
                .message("Congrats! +" + baseXpInt + " XP earned. Post to unlock " + pendingXpInt + " more XP!");

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
        // 1. Validate Session & Post via Helper
        CookingSession session = helper.validateSessionForLinking(sessionId, userId);
        PostLinkInfo postData = helper.validateAndGetPost(request.getPostId(), userId);
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 2. Calculate XP for Linking
        int finalXpToAward = helper.calculateFinalXpForLinking(session, postData);

        // 3. Get badges from recipe (only awarded on post/link, not on complete)
        List<String> badgesEarned = (recipe.getRewardBadges() != null) 
                ? new ArrayList<>(recipe.getRewardBadges()) 
                : new ArrayList<>();

        // 4. Persist post XP before awarding side effects.
        // If this write fails, the link must fail rather than silently returning a post
        // that will display the wrong earned XP forever.
        try {
            postProvider.updatePostXp(request.getPostId(), finalXpToAward);
            log.info("Updated post {} with xpEarned={}", request.getPostId(), finalXpToAward);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to persist post xpEarned for post {}: {}", request.getPostId(), e.getMessage());
            throw new AppException(ErrorCode.POST_SERVICE_ERROR);
        }

        // 5. Process Side Effects (Stats, Kafka with badges, Creator Bonus)
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

        // 6. Update Session
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
        Optional<CookingSession> sessionOpt = sessionRepository.findFirstByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS);

        if (sessionOpt.isEmpty()) return null;

        CookingSession session = sessionOpt.get();
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        helper.calculateRemainingTime(session); // Use helper
        return helper.mapToCurrentSessionResponse(session, recipe); // Use helper
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

        // Navigation logic kept here as it's tightly coupled with Session State
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

        // CRITICAL: Only PAUSED sessions can be resumed
        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        // Null guard for resumeDeadline — if null, allow resume (no deadline set)
        if (session.getResumeDeadline() != null && utcNow().isAfter(session.getResumeDeadline())) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setPausedAt(null);
        session.setResumeDeadline(null);
        sessionRepository.save(session);

        // Re-set cooking presence after resume
        Recipe recipe = recipeRepository.findById(session.getRecipeId()).orElse(null);
        setActiveCookingPresence(userId, session, recipe);

        return SessionResumeResponse.builder()
                .sessionId(session.getId())
                .status(SessionStatus.IN_PROGRESS)
                .resumeAt(utcNow())
                .build();
    }

    /**
     * Mark a step as completed (add to completedSteps array).
     * This is separate from navigation — users can complete steps in any order.
     * Idempotent: completing an already-completed step returns success without duplicating.
     */
    @Transactional
    public CompleteStepResponse completeStep(String sessionId, CompleteStepRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // CRITICAL: Only allow step completion on IN_PROGRESS sessions
        // Previous check only blocked COMPLETED — PAUSED and ABANDONED sessions could still complete steps
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        int totalSteps = recipe.getSteps().size();
        int stepNumber = request.getStepNumber();

        // Validate step number is within range
        if (stepNumber < 1 || stepNumber > totalSteps) {
            throw new AppException(ErrorCode.INVALID_TARGET_STEP);
        }

        // Initialize completedSteps if null
        if (session.getCompletedSteps() == null) {
            session.setCompletedSteps(new ArrayList<>());
        }

        // Check if already completed (idempotent)
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
     * Abandon a cooking session.
     * Sets status to ABANDONED. Cannot be resumed.
     */
    @Transactional
    public SessionAbandonResponse abandonSession(String sessionId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // Can't abandon already completed, posted, or already abandoned sessions
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus().hasClaimedPostXp()) {
            throw new AppException(ErrorCode.SESSION_COMPLETED);
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            // Idempotent - return success if already abandoned
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

    // This method only serves FeignClient, simple logic so kept here
    public CookingSession getSessionById(String sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }

    // ======================== Friends Cooking Now ========================

    /**
     * Returns active cooking sessions for people the current user follows.
     * Uses Redis MGET for O(1) per-friend lookup in a single round-trip.
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

    // ======================== Presence Helpers ========================

    /**
     * Set Redis presence for "Friends Cooking Now" feature.
     * Non-critical — failures are logged but don't block the session operation.
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
     * Remove Redis presence. Non-critical — TTL will clean up even if this fails.
     */
    private void removeActiveCookingPresence(String userId) {
        try {
            activeCookingRepository.removeActive(userId);
        } catch (Exception e) {
            log.warn("Failed to remove cooking presence for user {}: {}", userId, e.getMessage());
        }
    }

    // ======================== Cook Card ========================

    /**
     * Aggregates session + recipe + profile data into a single DTO
     * for the shareable cook card feature.
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

        // Recipe info for difficulty + total steps
        String difficulty = null;
        Integer totalSteps = null;
        Recipe recipe = recipeRepository.findById(session.getRecipeId()).orElse(null);
        if (recipe != null) {
            difficulty = recipe.getDifficulty() != null ? recipe.getDifficulty().getValue() : null;
            totalSteps = recipe.getSteps() != null ? recipe.getSteps().size() : null;
        }

        // Profile info
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

        // Cooking time
        Long cookingTimeMinutes = null;
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            cookingTimeMinutes = java.time.Duration.between(session.getStartedAt(), session.getCompletedAt()).toMinutes();
        }

        // Total XP (base + remaining/post bonus)
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