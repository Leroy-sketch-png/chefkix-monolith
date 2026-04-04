package com.chefkix.culinary.common.helper;

import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.culinary.features.session.dto.request.TimerEventRequest;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.Step;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.enums.TimerEventType;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.features.session.dto.response.CurrentSessionResponse;
import com.chefkix.culinary.features.session.dto.response.StartSessionResponse;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.social.api.PostProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeHelper {

    private final RecipeRepository recipeRepository;
    private final CookingSessionRepository sessionRepository;
    private final PostProvider postProvider;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

    // --- CALCULATION & ANTI-CHEAT LOGIC ---

    public void calculateRemainingTime(CookingSession session) {
        if (session.getActiveTimers() == null || session.getActiveTimers().isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();

        for (CookingSession.ActiveTimer timer : session.getActiveTimers()) {
            long elapsedSeconds = ChronoUnit.SECONDS.between(timer.getStartedAt(), now);
            int remaining = (int) (timer.getTotalSeconds() - elapsedSeconds);
            timer.setRemainingSeconds(Math.max(0, remaining));
        }
    }

    public void validateAntiCheat(CookingSession session, Recipe recipe) {
        long actualSeconds = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
        long minSeconds = (long) (recipe.getCookTimeMinutes() * 60 * 0.5);

        if (actualSeconds < minSeconds) {
            session.setFlagged(true);
            session.setFlagReason("Cooked too fast: " + actualSeconds + "s (min: " + minSeconds + "s)");
            log.warn("Anti-cheat: User {} flagged for fast cooking session {}", session.getUserId(), session.getId());
        }
    }

    public double calculateMasteryMultiplier(String userId, String recipeId) {
        // Count COMPLETED + POSTED sessions (not just POSTED)
        // Previously only counted POSTED, which meant users could bypass mastery decay by never posting
        long completedCount = sessionRepository.countByUserIdAndRecipeIdAndStatus(userId, recipeId, SessionStatus.COMPLETED);
        long postedCount = sessionRepository.countByUserIdAndRecipeIdAndStatus(userId, recipeId, SessionStatus.POSTED);
        long cookCount = completedCount + postedCount;
        double mult;
        if (cookCount == 0) mult = 1.0;
        else if (cookCount == 1) mult = 0.5;
        else if (cookCount == 2) mult = 0.25;
        else mult = 0.0;
        
        return mult;
    }

    public int calculateFinalXpForLinking(CookingSession session, PostLinkInfo postData) {
        int photoCount = postData.getPhotoCount();
        double photoMult = (photoCount >= 2) ? 1.0 : 0.5;

        long daysSinceCompletion = ChronoUnit.DAYS.between(session.getCompletedAt(), LocalDateTime.now());
        double decayMult;
        if (daysSinceCompletion <= 7) decayMult = 1.0;
        else if (daysSinceCompletion <= 14) decayMult = 0.5;
        else decayMult = 0.0;

        double pendingXpBase = (session.getPendingXp() != null) ? session.getPendingXp() : 0;
        int finalXp = (int) (pendingXpBase * photoMult * decayMult);
        
        return finalXp;
    }

    // --- LOGIC TIMER ---

    public void handleTimerStart(CookingSession session, Recipe recipe, int stepNumber, LocalDateTime now) {
        int duration = recipe.getSteps().stream()
                .filter(s -> s.getStepNumber() == stepNumber)
                .findFirst()
                .map(Step::getTimerSeconds)
                .orElse(0);

        if (duration > 0) {
            boolean alreadyRunning = session.getActiveTimers().stream()
                    .anyMatch(t -> t.getStepNumber() == stepNumber);

            if (!alreadyRunning) {
                CookingSession.ActiveTimer newTimer = CookingSession.ActiveTimer.builder()
                        .stepNumber(stepNumber)
                        .totalSeconds(duration)
                        .startedAt(now)
                        .remainingSeconds(duration)
                        .build();
                session.getActiveTimers().add(newTimer);
            }
        }
    }

    public void handleTimerStop(CookingSession session, int stepNumber) {
        session.getActiveTimers().removeIf(t -> t.getStepNumber() == stepNumber);
        if (!session.getCompletedSteps().contains(stepNumber)) {
            session.getCompletedSteps().add(stepNumber);
        }

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (stepNumber < 1 || stepNumber > recipe.getSteps().size()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Invalid step number");
        }

        if (stepNumber >= session.getCurrentStep()) {
            int nextStep = Math.min(stepNumber + 1, recipe.getSteps().size());
            session.setCurrentStep(nextStep);
        }
    }

    public TimerEventType validateAndParseTimerEvent(String userId, CookingSession session, TimerEventRequest request) {
        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return TimerEventType.valueOf(request.getEvent().toString());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Event must be: start, complete, or skip");
        }
    }

    // --- LOGIC VALIDATION ---

    public CookingSession validateSessionForLinking(String sessionId, String userId) {
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        if (session.getStatus() == SessionStatus.POSTED) throw new AppException(ErrorCode.SESSION_ALREADY_LINKED);
        if (session.getStatus() != SessionStatus.COMPLETED) throw new AppException(ErrorCode.INVALID_ACTION, "Session must be COMPLETED");

        return session;
    }

    public PostLinkInfo validateAndGetPost(String postId, String userId) {
        try {
            PostLinkInfo postData = postProvider.getPostLinking(postId);
            if (postData == null) throw new AppException(ErrorCode.POST_NOT_FOUND);

            if (!postData.getUserId().equals(userId)) {
                throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION, "You can only link your own posts.");
            }
            return postData;
        } catch (AppException e) {
            throw e; // re-throw our own exceptions
        } catch (Exception e) {
            log.error("Post Service Error: {}", e.getMessage());
            throw new AppException(ErrorCode.POST_SERVICE_ERROR);
        }
    }

    // --- LOGIC SIDE EFFECTS (DB/KAFKA) ---

    public void updateRecipeStats(String recipeId, int cookCountInc, int xpEarnedInc) {
        Query query = Query.query(Criteria.where("_id").is(recipeId));
        Update update = new Update()
                .inc("cookCount", cookCountInc)
                .inc("creatorXpEarned", xpEarnedInc)
                .set("lastCookedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Recipe.class);
    }

    public void sendXpEvent(String userId, double amount, String source, String sessionId, String desc) {
        sendXpEventFull(userId, amount, source, sessionId, desc, null, false);
    }

    public void sendXpEventWithBadges(String userId, double amount, String source, String sessionId, String desc, List<String> badges) {
        sendXpEventFull(userId, amount, source, sessionId, desc, badges, false);
    }
    
    public void sendXpEventWithChallenge(String userId, double amount, String source, String sessionId, String desc, boolean challengeCompleted) {
        sendXpEventFull(userId, amount, source, sessionId, desc, null, challengeCompleted);
    }

    public void sendXpEventFull(String userId, double amount, String source, String sessionId, String desc, 
                                List<String> badges, boolean challengeCompleted) {
        if (amount <= 0 && (badges == null || badges.isEmpty()) && !challengeCompleted) return;
        XpRewardEvent xpEvent = XpRewardEvent.builder()
                .userId(userId)
                .amount(amount)
                .source(source)
                .sessionId(sessionId)
                .description(desc)
                .badges(badges)
                .challengeCompleted(challengeCompleted)
                .build();
        kafkaTemplate.send("xp-delivery", xpEvent);
        log.info("Sent XP event: userId={}, amount={}, source={}, badges={}, challengeCompleted={}", 
                userId, amount, source, badges, challengeCompleted);
    }

    // sendBadgeEvent is now deprecated - use sendXpEventWithBadges instead
    public void sendBadgeEvent(String userId, List<String> badges, String sessionId, String recipeId) {
        // Badges are now sent via XP event
        log.warn("sendBadgeEvent called but badges should be sent with XP event. userId={}, badges={}", userId, badges);
    }

    public boolean processCreatorBonus(Recipe recipe, String cookerId, String sessionId) {
        if (recipe.getUserId().equals(cookerId)) return false;

        int bonusAmount = (int) (recipe.getXpReward() * 0.04);
        if (bonusAmount > 0) {
            XpRewardEvent xpEvent = XpRewardEvent.builder()
                    .userId(recipe.getUserId())
                    .amount(bonusAmount)
                    .recipeId(recipe.getId())
                    .sessionId(sessionId)
                    .source("CREATOR_BONUS")
                    .description("Bonus from others cooking: " + recipe.getTitle())
                    .build();
            kafkaTemplate.send("xp-delivery", xpEvent);
            return true;
        }
        return false;
    }

    /**
     * Increment/decrement multiple stats at once.
     * @param recipeId Recipe ID
     * @param statsIncrements Map<Field name, Increment amount> (e.g., "wins" -> 1, "exp" -> 100)
     */
    public void incrementRecipeStats(String recipeId, Map<String, Number> statsIncrements) {
        if (statsIncrements == null || statsIncrements.isEmpty()) {
            return;
        }

        Query query = Query.query(Criteria.where("_id").is(recipeId));
        Update update = new Update();

        // Iterate through map and create inc commands for each field
        // Automatically adds "statistics." prefix so calling code is cleaner
        statsIncrements.forEach(update::inc);

        // Only call DB once
        mongoTemplate.updateFirst(query, update, "recipes");
    }

    // --- MAPPING (Could use MapStruct but keeping logic here to keep Service clean) ---

    public StartSessionResponse mapToStartResponse(CookingSession session, Recipe recipe) {
        return StartSessionResponse.builder()
                .sessionId(session.getId())
                .recipeId(session.getRecipeId())
                .startedAt(session.getStartedAt())
                .status(session.getStatus().name().toLowerCase())
                .currentStep(session.getCurrentStep())
                .totalSteps(recipe != null && recipe.getSteps() != null ? recipe.getSteps().size() : 0)
                .activeTimers(session.getActiveTimers() != null ? new ArrayList<>(session.getActiveTimers()) : new ArrayList<>())
                .recipe(recipe != null ? StartSessionResponse.RecipeInfo.builder()
                        .id(recipe.getId())
                        .title(recipe.getTitle())
                        .xpReward(recipe.getXpReward())
                        .cookTimeMinutes(recipe.getCookTimeMinutes())
                        .build() : null)
                .build();
    }

    public CurrentSessionResponse mapToCurrentSessionResponse(CookingSession session, Recipe recipe) {
        if (recipe == null) return null;
        
        // Build recipe info with all needed fields
        CurrentSessionResponse.SessionRecipeInfo recipeDto = CurrentSessionResponse.SessionRecipeInfo.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .totalSteps(recipe.getSteps() != null ? recipe.getSteps().size() : 0)
                .xpReward(recipe.getXpReward())
                .coverImageUrl(recipe.getCoverImageUrl())
                .build();

        // Calculate days remaining until post deadline (if applicable)
        Integer daysRemaining = null;
        if (session.getPostDeadline() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDateTime.now(), 
                session.getPostDeadline()
            );
            daysRemaining = (int) Math.max(0, days);
        }

        return CurrentSessionResponse.builder()
                .sessionId(session.getId())
                .recipeId(session.getRecipeId())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .status(session.getStatus())
                .currentStep(session.getCurrentStep())
                .completedSteps(session.getCompletedSteps())
                .activeTimers(session.getActiveTimers())
                .recipe(recipeDto)
                // XP tracking fields (populated after completion)
                .baseXpAwarded(session.getBaseXpAwarded() != null ? session.getBaseXpAwarded().intValue() : null)
                .pendingXp(session.getPendingXp() != null ? session.getPendingXp().intValue() : null)
                .remainingXpAwarded(session.getRemainingXpAwarded() != null ? session.getRemainingXpAwarded().intValue() : null)
                // Post linking fields
                .postId(session.getPostId())
                .postDeadline(session.getPostDeadline())
                .daysRemaining(daysRemaining)
                .build();
    }
}