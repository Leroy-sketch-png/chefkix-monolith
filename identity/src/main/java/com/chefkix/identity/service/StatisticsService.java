package com.chefkix.identity.service;

import com.chefkix.shared.event.GamificationNotificationEvent;
import com.chefkix.shared.event.ReminderEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.identity.dto.response.LeaderboardResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.enums.Title;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.UserProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.dto.CreatorInsightsInfo;
import lombok.AccessLevel;
import org.springframework.context.annotation.Lazy;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public class StatisticsService implements StatisticsCounterOperations {

  private static final String GAMIFICATION_TOPIC = "gamification-delivery";
  private static final String REMINDER_TOPIC = "reminder-delivery";

  MongoTemplate mongoTemplate;
  UserProfileRepository userProfileRepository;
  ProfileMapper profileMapper;
  KafkaTemplate<String, Object> kafkaTemplate;
  SettingsService settingsService;
  BlockService blockService;
  KafkaIdempotencyService idempotencyService;

  @Lazy RecipeProvider recipeProvider;

  /**
   * Main method handling logic after completing a Recipe (Called from Recipe Service). Performs: Add
   * XP + Check Level + Add Badges + Increment Counter -> Save once.
   */
  @Transactional
  @Retryable(
      retryFor = {OptimisticLockingFailureException.class},
      maxAttempts = 5)
  public RecipeCompletionResponse updateAfterCompletion(InternalCompletionRequest request) {
    // Internal method called by culinary module via ProfileProvider.
    // Prefer request.userId over SecurityContext since auth may be null for internal calls.
    String userId = request.getUserId();
        String idempotencyKey = request.getIdempotencyKey();
        boolean reservedSyncIdempotency = false;
    if (userId == null || userId.isEmpty()) {
      // Fallback to SecurityContext only if userId not provided in request
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null) {
        userId = auth.getName();
      } else {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
    }

        try {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                reservedSyncIdempotency = idempotencyService.tryProcess(idempotencyKey, "xp-delivery");
                if (!reservedSyncIdempotency) {
                    log.info("Duplicate completion request detected for user {} with key {}", userId, idempotencyKey);
                    UserProfile existingProfile =
                            userProfileRepository
                                    .findByUserId(userId)
                                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    Statistics existingStats =
                            (existingProfile.getStatistics() != null)
                                    ? existingProfile.getStatistics()
                                    : Statistics.builder().build();
                    int currentLevel =
                            existingStats.getCurrentLevel() != null ? existingStats.getCurrentLevel() : 1;
                    return buildCompletionResponse(
                            userId,
                            existingStats,
                            new XpLevelResult(false, currentLevel, currentLevel, null));
        }
      }

            // 1. Get Profile
            UserProfile profile =
                    userProfileRepository
                            .findByUserId(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            Statistics stats =
                    (profile.getStatistics() != null) ? profile.getStatistics() : Statistics.builder().build();

            // 2. PROCESS XP & LEVEL (Using private helper) - capture result for level-up detection
            XpLevelResult levelResult = applyXpAndLevelLogic(stats, request.getXpAmount());

            // 3. PROCESS BADGES (Add new ones, avoid duplicates)
            List<String> actuallyAddedBadges = new ArrayList<>();
            if (request.getNewBadges() != null && !request.getNewBadges().isEmpty()) {
                // Initialize badge list if not yet created
                if (stats.getBadges() == null) {
                    stats.setBadges(new ArrayList<>());
                }
                if (stats.getBadgeTimestamps() == null) {
                    stats.setBadgeTimestamps(new HashMap<>());
                }

                for (String badge : request.getNewBadges()) {
                    // Only add if user doesn't already have this badge
                    if (!stats.getBadges().contains(badge)) {
                        stats.getBadges().add(badge);
                        stats.getBadgeTimestamps().putIfAbsent(badge, java.time.Instant.now());
                        actuallyAddedBadges.add(badge);
                    }
                }
            }

                // 4. APPLY COOKING COMPLETION PROGRESSION
                // Keep sync completion behavior aligned with the Kafka fallback path:
                // streaks, recipe mastery, completion count, and challenge streaks.
                applyCookingCompletionProgress(
                    stats,
                    userId,
                    request.getRecipeId(),
                    request.isChallengeCompleted(),
                    Instant.now());

            // 5. UPDATE AND SAVE (once)
            profile.setStatistics(stats);
            userProfileRepository.save(profile);

            log.info(
                    "Recipe Completed for User {}: +{} XP, New Level: {}, Added Badges: {}",
                    userId,
                    request.getXpAmount(),
                    stats.getCurrentLevel(),
                    actuallyAddedBadges);

            return buildCompletionResponse(userId, stats, levelResult);
        } catch (RuntimeException e) {
            if (reservedSyncIdempotency && idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.removeProcessed(idempotencyKey, "xp-delivery");
            }
            throw e;
        }
  }

  /** Manually add XP (Admin or other events) */
  @Transactional
  @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
  public ProfileResponse addXp(String userId, double xpAmount) {
      processXpAndStatsUpdate(userId, xpAmount, null, false);

      // Fetch profile again to map to response
      UserProfile profile = userProfileRepository.findByUserId(userId)
              .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
      return profileMapper.toProfileResponse(profile);
  }

  // --- PRIVATE HELPER METHODS (Optimized for reuse) ---

  /**
   * Result of XP and level calculation, used for notification triggering.
   */
  private record XpLevelResult(boolean leveledUp, int previousLevel, int newLevel, String newTitle) {}

    private RecipeCompletionResponse buildCompletionResponse(
            String userId, Statistics stats, XpLevelResult levelResult) {
        double currentXp = stats.getCurrentXP() != null ? stats.getCurrentXP() : 0.0;
        double currentXpGoal = stats.getCurrentXPGoal() != null ? stats.getCurrentXPGoal() : 1000.0;
        int currentLevel = stats.getCurrentLevel() != null ? stats.getCurrentLevel() : 1;
        long completionCount = stats.getCompletionCount() != null ? stats.getCompletionCount() : 0L;
        int xpToNextLevel = (int) Math.max(0, Math.round(currentXpGoal - currentXp));

        return RecipeCompletionResponse.builder()
                .userId(userId)
                .currentXP((int) Math.round(currentXp))
                .currentXPGoal((int) Math.round(currentXpGoal))
                .currentLevel(currentLevel)
                .completionCount(completionCount)
                .leveledUp(levelResult.leveledUp())
                .oldLevel(levelResult.previousLevel())
                .newLevel(levelResult.newLevel())
                .xpToNextLevel(xpToNextLevel)
                .build();
    }

    /**
   * Core logic for calculating excess XP and leveling up. This method only modifies the Statistics
   * object, does not call DB.
   * 
   * @return XpLevelResult with level change info for notifications
   */
  private XpLevelResult applyXpAndLevelLogic(Statistics stats, double xpAmount) {
    if (xpAmount < 0) {
      throw new IllegalArgumentException("XP amount cannot be negative: " + xpAmount);
    }
    if (xpAmount == 0) {
      return new XpLevelResult(false, stats.getCurrentLevel(), stats.getCurrentLevel(), null);
    }
    int previousLevel = stats.getCurrentLevel();
    stats.setCurrentXP(stats.getCurrentXP() + xpAmount);

    // Track weekly and monthly XP for leaderboard
    double currentWeeklyXp = stats.getXpWeekly() != null ? stats.getXpWeekly() : 0.0;
    double currentMonthlyXp = stats.getXpMonthly() != null ? stats.getXpMonthly() : 0.0;
    stats.setXpWeekly(currentWeeklyXp + xpAmount);
    stats.setXpMonthly(currentMonthlyXp + xpAmount);

    boolean leveledUp = false;

    // Loop to check level up (can level up multiple times at once)
    while (stats.getCurrentXP() >= stats.getCurrentXPGoal()) {
      leveledUp = true;
      double excessXp = stats.getCurrentXP() - stats.getCurrentXPGoal();
      stats.setCurrentLevel(stats.getCurrentLevel() + 1);
      stats.setCurrentXP(excessXp);
      stats.setCurrentXPGoal(calculateNewXpGoal(stats.getCurrentLevel()));
    }

    String newTitle = null;
    if (leveledUp) {
      stats.setTitle(getTitleForLevel(stats.getCurrentLevel()));
      newTitle = stats.getTitle().name();
    }

    return new XpLevelResult(leveledUp, previousLevel, stats.getCurrentLevel(), newTitle);
  }

    private void applyCookingCompletionProgress(
            Statistics stats,
            String userId,
            String recipeId,
            boolean challengeCompleted,
            Instant now) {
        applyCookingStreakProgress(stats, userId, now);
        trackRecipeCookProgress(stats, userId, recipeId);
        stats.setLastCookAt(now);

        long currentCount = stats.getCompletionCount() == null ? 0L : stats.getCompletionCount();
        stats.setCompletionCount(currentCount + 1);

        if (challengeCompleted) {
            applyChallengeCompletionProgress(stats, userId, now);
        }
    }

    private void applyCookingStreakProgress(Statistics stats, String userId, Instant now) {
        Instant lastCook = stats.getLastCookAt();

        if (lastCook == null) {
            stats.setStreakCount(1);
            log.info("User {} started streak: 1 (first cook)", userId);
        } else {
            Duration sinceLastCook = Duration.between(lastCook, now);
            long hoursSinceLastCook = sinceLastCook.toHours();

            if (hoursSinceLastCook <= 72) {
                int newStreak = (stats.getStreakCount() != null ? stats.getStreakCount() : 0) + 1;
                stats.setStreakCount(newStreak);
                log.info(
                        "User {} streak continued: {} ({}h since last cook)",
                        userId,
                        newStreak,
                        hoursSinceLastCook);
            } else {
                stats.setStreakCount(1);
                log.info(
                        "User {} streak reset to 1 ({}h since last cook, exceeded 72h)",
                        userId,
                        hoursSinceLastCook);
            }
        }

        int currentStreak = stats.getStreakCount() != null ? stats.getStreakCount() : 0;
        int previousLongest = stats.getLongestStreak() != null ? stats.getLongestStreak() : 0;
        if (currentStreak > previousLongest) {
            stats.setLongestStreak(currentStreak);
            log.info("User {} new longest streak record: {}", userId, currentStreak);
        }
    }

    private void trackRecipeCookProgress(Statistics stats, String userId, String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return;
        }

        Map<String, Integer> cookCounts = stats.getRecipeCookCounts();
        if (cookCounts == null) {
            cookCounts = new HashMap<>();
            stats.setRecipeCookCounts(cookCounts);
        }

        int newCount = cookCounts.merge(recipeId, 1, Integer::sum);
        stats.setRecipesCooked((long) cookCounts.size());
        stats.setRecipesMastered(cookCounts.values().stream().filter(c -> c >= 5).count());
        log.info(
                "User {} cooked recipe {} (count: {}, total distinct: {}, mastered: {})",
                userId,
                recipeId,
                newCount,
                stats.getRecipesCooked(),
                stats.getRecipesMastered());
    }

    private void applyChallengeCompletionProgress(Statistics stats, String userId, Instant now) {
        Instant lastChallenge = stats.getLastChallengeAt();

        if (lastChallenge == null) {
            stats.setChallengeStreak(1);
            log.info("User {} started challenge streak: 1 (first challenge)", userId);
        } else {
            Duration sinceLastChallenge = Duration.between(lastChallenge, now);
            long hoursSinceLastChallenge = sinceLastChallenge.toHours();

            if (hoursSinceLastChallenge <= 48) {
                int newChallengeStreak =
                        (stats.getChallengeStreak() != null ? stats.getChallengeStreak() : 0) + 1;
                stats.setChallengeStreak(newChallengeStreak);
                log.info(
                        "User {} challenge streak continued: {} ({}h since last)",
                        userId,
                        newChallengeStreak,
                        hoursSinceLastChallenge);
            } else {
                stats.setChallengeStreak(1);
                log.info(
                        "User {} challenge streak reset to 1 ({}h since last, exceeded 48h)",
                        userId,
                        hoursSinceLastChallenge);
            }
        }

        stats.setLastChallengeAt(now);
    }

  private Title getTitleForLevel(int level) {
    if (level >= 40) return Title.SEMIPRO;
    if (level >= 20) return Title.AMATEUR;
    return Title.BEGINNER;
  }

  private double calculateNewXpGoal(int currentLevel) {
    return (int) (1000 * Math.pow(1.1, currentLevel));
  }

  private static final Set<String> ALLOWED_COUNTER_FIELDS = Set.of(
      "totalRecipesPublished", "friendCount", "followerCount", "followingCount",
      "friendRequestCount", "recipesCookedCount", "postsCreatedCount");

  public void incrementCounter(String userId, String fieldName, int amount) {
    if (!ALLOWED_COUNTER_FIELDS.contains(fieldName)) { 
      throw new AppException(ErrorCode.INVALID_OPERATION);
    }
    String field = "statistics." + fieldName;
    Query query = Query.query(Criteria.where("userId").is(userId));
    Update update = new Update().inc(field, amount);
    mongoTemplate.updateFirst(query, update, "user_profiles");

    // Floor at 0 to prevent negative counters from data inconsistencies
    if (amount < 0) {
      Query floorQuery = Query.query(Criteria.where("userId").is(userId).and(field).lt(0));
      Update floorUpdate = new Update().set(field, 0);
      mongoTemplate.updateFirst(floorQuery, floorUpdate, "user_profiles");
    }
  }

    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void rewardXp(String userId, double amount) {
        // Legacy method - delegates to full method with no badges
        rewardXpFull(userId, amount, null, false);
    }

    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void rewardXpWithBadges(String userId, double amount, List<String> badges) {
        rewardXpFull(userId, amount, badges, false);
    }

    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void rewardXpFull(String userId, double amount, List<String> badges, boolean challengeCompleted) {
        rewardXpFull(userId, amount, badges, challengeCompleted, null, "COOKING_SESSION", null);
    }

    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void rewardXpFull(String userId, double amount, List<String> badges, boolean challengeCompleted, String recipeId) {
        rewardXpFull(userId, amount, badges, challengeCompleted, recipeId, "COOKING_SESSION", null);
    }

    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void rewardXpFull(
            String userId,
            double amount,
            List<String> badges,
            boolean challengeCompleted,
            String recipeId,
            String source,
            String sessionId) {
        log.info("Kafka Event: Processing reward {} XP for user {} with badges {}, challengeCompleted={}, recipeId={}",
                amount, userId, badges, challengeCompleted, recipeId);

        String normalizedSource = (source != null && !source.isBlank()) ? source : "COOKING_SESSION";

        // Get profile and stats
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Statistics stats = (profile.getStatistics() != null) ? profile.getStatistics() : Statistics.builder().build();

        // Apply XP and level logic - capture result for notifications
        XpLevelResult levelResult = applyXpAndLevelLogic(stats, amount);

        // Apply badges (deduplicate) and track new badges for notification
        List<String> actuallyNewBadges = new ArrayList<>();
        if (badges != null && !badges.isEmpty()) {
            Set<String> currentBadges = new HashSet<>(stats.getBadges() != null ? stats.getBadges() : new ArrayList<>());
            if (stats.getBadgeTimestamps() == null) {
                stats.setBadgeTimestamps(new HashMap<>());
            }
            int beforeCount = currentBadges.size();
            for (String badge : badges) {
                if (!currentBadges.contains(badge)) {
                    actuallyNewBadges.add(badge);
                    stats.getBadgeTimestamps().putIfAbsent(badge, java.time.Instant.now());
                }
            }
            currentBadges.addAll(badges);
            stats.setBadges(new ArrayList<>(currentBadges));
            int newBadgesCount = currentBadges.size() - beforeCount;
            if (newBadgesCount > 0) {
                log.info("User {} earned {} new badges: {}", userId, newBadgesCount, actuallyNewBadges);
            }
        }

        if ("COOKING_SESSION".equals(normalizedSource)) {
            Instant now = Instant.now();
            applyCookingCompletionProgress(stats, userId, recipeId, challengeCompleted, now);
        }

        // Save
        profile.setStatistics(stats);
        userProfileRepository.save(profile);

        // Send gamification notification (XP, level-up, badges)
        sendGamificationNotification(
                userId, profile.getDisplayName(),
                amount, stats.getCurrentXP(),
                levelResult, actuallyNewBadges,
            normalizedSource, recipeId, sessionId);
    }

    private Statistics processXpAndStatsUpdate(String userId, double xpAmount, List<String> newBadges, boolean incrementRecipeCount) {
        // A. Get Profile (Fail-safe)
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Statistics stats = (profile.getStatistics() != null) ? profile.getStatistics() : Statistics.builder().build();

        // B. PROCESS XP & LEVEL (Core logic - fixed below)
        applyXpAndLevelLogic(stats, xpAmount);

        // C. PROCESS BADGES (Using Set for concise code and deduplication)
        if (newBadges != null && !newBadges.isEmpty()) {
            Set<String> currentBadges = new HashSet<>(stats.getBadges() != null ? stats.getBadges() : new ArrayList<>());
            if (stats.getBadgeTimestamps() == null) {
                stats.setBadgeTimestamps(new HashMap<>());
            }
            for (String badge : newBadges) {
                if (!currentBadges.contains(badge)) {
                    stats.getBadgeTimestamps().putIfAbsent(badge, java.time.Instant.now());
                }
            }
            currentBadges.addAll(newBadges);
            stats.setBadges(new ArrayList<>(currentBadges));
        }

        // D. PROCESS COUNTER (Recipes cooked count)
        if (incrementRecipeCount) {
            long currentCount = stats.getCompletionCount() == null ? 0L : stats.getCompletionCount();
            stats.setCompletionCount(currentCount + 1);
        }

        // E. SAVE TO DATABASE (MongoDB)
        profile.setStatistics(stats);
        userProfileRepository.save(profile);

        return stats;
    }

    /**
     * Handle creator rewards (called when someone else cooks their recipe).
     * Logic:
     * 1. Tracking: Increment xpEarnedAsCreator & totalCooks (for stats display).
     * 2. Gamification: Increment currentXP & Calculate Level (for user leveling).
     */
    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void applyCreatorReward(String creatorId, double xpAmount) {
        log.info("Applying Creator Reward for User {}: +{} XP", creatorId, xpAmount);

        // 1. Get Profile (Fail-safe: create new if not exists)
        UserProfile profile = userProfileRepository.findByUserId(creatorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Statistics stats = (profile.getStatistics() != null)
                ? profile.getStatistics()
                : Statistics.builder().build();

        // --- PART 1: TRACKING (Stats only, does not affect level) ---
        // Accumulate XP earned as a Creator
        long creatorXpToAdd = (long) xpAmount;
        long currentCreatorXp = stats.getXpEarnedAsCreator() == null ? 0L : stats.getXpEarnedAsCreator();
        stats.setXpEarnedAsCreator(currentCreatorXp + creatorXpToAdd);

        // Increment count of others who cooked your recipes
        long currentTotalCooks = stats.getTotalCooksOfYourRecipes() == null ? 0L : stats.getTotalCooksOfYourRecipes();
        stats.setTotalCooksOfYourRecipes(currentTotalCooks + 1);

        // Weekly tracking (if applicable)
        stats.setWeeklyCreatorCooks(stats.getWeeklyCreatorCooks() + 1);
        stats.setWeeklyCreatorXp(stats.getWeeklyCreatorXp() + creatorXpToAdd);


        // --- PART 2: GAMIFICATION (Directly affects user level) ---
        // Reuse the private helper to add currentXP and calculate Level Up
        // This will auto: currentXP += xpAmount and check while (currentXP >= goal)
        XpLevelResult levelResult = applyXpAndLevelLogic(stats, xpAmount);


        // --- SAVE TO DATABASE ---
        profile.setStatistics(stats);
        userProfileRepository.save(profile);

        // --- SEND NOTIFICATION: Level-up via gamification topic ---
        if (levelResult.leveledUp()) {
            sendGamificationNotification(
                    creatorId, profile.getDisplayName(),
                    xpAmount, stats.getCurrentXP(),
                    levelResult, null,
                    "CREATOR_BONUS", null, null);
        }

        // --- ALWAYS NOTIFY CREATOR: Someone cooked your recipe! ---
        try {
            ReminderEvent creatorNotif = ReminderEvent.builder()
                    .userId(creatorId)
                    .displayName(profile.getDisplayName())
                    .reminderType("CREATOR_BONUS")
                    .content(String.format(
                            "\uD83C\uDF89 Someone cooked your recipe! You earned +%.0f XP as a creator bonus.",
                            xpAmount))
                    .priority(ReminderEvent.ReminderPriority.NORMAL)
                    .build();
            kafkaTemplate.send(REMINDER_TOPIC, creatorNotif);
            log.info("Creator bonus notification sent to user {}: +{} XP", creatorId, xpAmount);
        } catch (Exception e) {
            log.error("Failed to send creator bonus notification to user {}", creatorId, e);
        }
    }

    /**
     * Awards XP for social engagement (likes, comments, saves).
     * Applies XP + level logic only — does NOT touch cooking streaks, completionCount, or challenge streak.
     */
    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void rewardSocialXp(String userId, double amount, String source) {
        log.info("Processing social XP for user {}: +{} XP (source={})", userId, amount, source);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Statistics stats = (profile.getStatistics() != null) ? profile.getStatistics() : Statistics.builder().build();

        XpLevelResult levelResult = applyXpAndLevelLogic(stats, amount);

        profile.setStatistics(stats);
        userProfileRepository.save(profile);

        if (levelResult.leveledUp()) {
            sendGamificationNotification(
                    userId, profile.getDisplayName(),
                    amount, stats.getCurrentXP(),
                    levelResult, null,
                    source, null, null);
        }
    }

    public CreatorStatsResponse getMyCreatorStats(String userId) {
        // 1. GET AGGREGATE STATS (From Local DB - Very fast)
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("Getting My Creator Stats for user {}", userId);
        Statistics stats = profile.getStatistics();

        // 2. Get detailed insights from culinary module (direct call in monolith)
        CreatorStatsResponse.TopRecipeDto topRecipe = null;
        List<CreatorStatsResponse.TopRecipeDto> highPerformingRecipes = new ArrayList<>();
        Double avgRating = null;

        try {
            CreatorInsightsInfo insights = recipeProvider.getCreatorInsights(userId);
            if (insights != null) {
                avgRating = insights.getAvgRating();
                if (insights.getTopRecipes() != null && !insights.getTopRecipes().isEmpty()) {
                    topRecipe = mapTopRecipe(insights.getTopRecipes().get(0));
                }
                if (insights.getHighPerformingRecipes() != null) {
                    highPerformingRecipes = insights.getHighPerformingRecipes().stream()
                            .map(this::mapTopRecipe)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch creator insights from culinary module", e);
            // Fallback: Still return aggregate stats, just missing top recipes
        }

        // 3. CALCULATE BADGES
        List<CreatorStatsResponse.CreatorBadgeDto> badges = calculateBadges(stats.getTotalRecipesPublished(), highPerformingRecipes);

        // 4. BUILD RESPONSE (per vision_and_spec/03-social.txt)
        return CreatorStatsResponse.builder()
                        .totalRecipesPublished(stats.getTotalRecipesPublished())
                        .totalCooksOfYourRecipes(stats.getTotalCooksOfYourRecipes())
                        .xpEarnedAsCreator(stats.getXpEarnedAsCreator())
                        .avgRating(avgRating)
                        .topRecipe(topRecipe)
                        .thisWeek(CreatorStatsResponse.WeeklyStatsDto.builder()
                                .newCooks(stats.getWeeklyCreatorCooks())
                                .xpEarned(stats.getWeeklyCreatorXp())
                                .build())
                        .creatorBadges(badges)
                .build();
    }

    private List<CreatorStatsResponse.CreatorBadgeDto> calculateBadges(long totalRecipes, List<CreatorStatsResponse.TopRecipeDto> recipes) {
        List<CreatorStatsResponse.CreatorBadgeDto> badges = new ArrayList<>();

        // Badge: Prolific Creator (Based on total posts - Local Data)
        if (totalRecipes >= 10) {
            badges.add(CreatorStatsResponse.CreatorBadgeDto.builder()
                    .name("Prolific Creator")
                    .icon("✍️")
                    .recipeTitle(null) // Badge is for the person, not the recipe
                    .build());
        }

        // Badge: Based on individual recipes (Remote Data)
        if (recipes != null) {
            for (CreatorStatsResponse.TopRecipeDto recipe : recipes) {
                long count = recipe.getCookCount();
                if (count >= 1000) {
                    badges.add(CreatorStatsResponse.CreatorBadgeDto.builder().name("Viral Recipe").icon("🔥").recipeTitle(recipe.getTitle()).build());
                } else if (count >= 100) {
                    badges.add(CreatorStatsResponse.CreatorBadgeDto.builder().name("100 Cooks Club").icon("🌟").recipeTitle(recipe.getTitle()).build());
                } else if (count >= 10) {
                    badges.add(CreatorStatsResponse.CreatorBadgeDto.builder().name("10 Cooks Club").icon("🍳").recipeTitle(recipe.getTitle()).build());
                }
            }
        }
        return badges;
    }

    /** Map culinary-api TopRecipeInfo to identity's CreatorStatsResponse.TopRecipeDto */
    private CreatorStatsResponse.TopRecipeDto mapTopRecipe(CreatorInsightsInfo.TopRecipeInfo info) {
        return CreatorStatsResponse.TopRecipeDto.builder()
                .id(info.getId())
                .title(info.getTitle())
                .coverImageUrl(info.getCoverImageUrl())
                .cookTimeMinutes(info.getCookTimeMinutes())
                .difficulty(info.getDifficulty())
                .cookCount(info.getCookCount())
                .xpGenerated((long) info.getXpGenerated())
                .averageRating(info.getAverageRating())
                .build();
    }

    // ==========================================
    // LEADERBOARD METHODS
    // ==========================================

    /**
     * Get leaderboard data.
     *
     * @param type Leaderboard type: "global", "friends", or "league"
     * @param timeframe Time period: "weekly", "monthly", or "all_time"
     * @param limit Max entries to return (default 50)
     * @param currentUserId Current user's ID for calculating their rank
     * @param friendIds List of friend IDs (for friends leaderboard)
     * @return LeaderboardResponse with entries and user's rank
     */
    public LeaderboardResponse getLeaderboard(String type, String timeframe, int limit, String currentUserId, List<String> friendIds) {
        limit = Math.min(limit, 100);
        log.info("Fetching {} {} leaderboard (limit: {}) for user {}", type, timeframe, limit, currentUserId);

        String sortField = switch (timeframe) {
            case "monthly" -> "statistics.xpMonthly";
            case "all_time" -> "statistics.currentXP";
            default -> "statistics.xpWeekly";
        };

        // 1. Get user profiles sorted by XP, with a ceiling to prevent DoS
        List<UserProfile> profiles;
        if ("friends".equals(type) && friendIds != null && !friendIds.isEmpty()) {
            profiles = userProfileRepository.findAllByUserIdIn(friendIds)
                    .stream()
                    .filter(p -> p.getStatistics() != null)
                    .collect(Collectors.toList());
            // Add current user to friends list for comparison
            userProfileRepository.findByUserId(currentUserId).ifPresent(profiles::add);
        } else {
            // Global leaderboard - top N users sorted by XP (fetch extra for privacy/block filtering)
            int fetchLimit = limit * 3;
            profiles = userProfileRepository.findAll(
                            PageRequest.of(0, fetchLimit, Sort.by(Sort.Direction.DESC, sortField)))
                    .getContent()
                    .stream()
                    .filter(p -> p.getStatistics() != null)
                    .collect(Collectors.toList());
        }

        // PRIVACY: Filter out users who opted out of leaderboard
        profiles.removeIf(p -> {
            if (p.getUserId().equals(currentUserId)) return false; // Never hide current user from themselves
            try {
                var privacy = settingsService.getPrivacySettingsByUserId(p.getUserId());
                return privacy != null && Boolean.FALSE.equals(privacy.getShowOnLeaderboard());
            } catch (Exception e) {
                return false; // Default to showing if settings lookup fails
            }
        });

        // PRIVACY: Filter out blocked users (bidirectional)
        List<String> invisibleIds = blockService.getInvisibleUserIds(currentUserId);
        if (!invisibleIds.isEmpty()) {
            profiles.removeIf(p -> invisibleIds.contains(p.getUserId()));
        }

        // 2. Sort by XP based on timeframe
        profiles.sort((a, b) -> {
            double xpA = getXpForTimeframe(a.getStatistics(), timeframe);
            double xpB = getXpForTimeframe(b.getStatistics(), timeframe);
            return Double.compare(xpB, xpA); // Descending order
        });

        // 3. Find current user's rank
        int userRank = 0;
        double userXp = 0.0;
        Double xpToNextRank = null;
        Integer nextRankPosition = null;
        Long userRecipesCooked = 0L;

        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getUserId().equals(currentUserId)) {
                userRank = i + 1;
                Statistics userStats = profiles.get(i).getStatistics();
                userXp = getXpForTimeframe(userStats, timeframe);
                userRecipesCooked = userStats.getCompletionCount() != null ? userStats.getCompletionCount() : 0L;

                // Calculate XP needed to reach next rank
                if (i > 0) {
                    double nextRankXp = getXpForTimeframe(profiles.get(i - 1).getStatistics(), timeframe);
                    xpToNextRank = nextRankXp - userXp + 1; // +1 to surpass, not tie
                    nextRankPosition = i; // Position they would reach
                }
                break;
            }
        }

        // 4. Build leaderboard entries (top N)
        AtomicInteger rankCounter = new AtomicInteger(1);
        List<LeaderboardResponse.LeaderboardEntry> entries = profiles.stream()
                .limit(limit)
                .map(profile -> {
                    Statistics stats = profile.getStatistics();
                    // Extract top 3 badges (most recently earned first)
                    List<String> topBadges = new ArrayList<>();
                    if (stats.getBadges() != null && !stats.getBadges().isEmpty()) {
                        var badges = stats.getBadges();
                        var timestamps = stats.getBadgeTimestamps();
                        if (timestamps != null && !timestamps.isEmpty()) {
                            // Sort by earn time descending, take top 3
                            topBadges = badges.stream()
                                    .sorted((a, b) -> {
                                        var ta = timestamps.getOrDefault(a, java.time.Instant.EPOCH);
                                        var tb = timestamps.getOrDefault(b, java.time.Instant.EPOCH);
                                        return tb.compareTo(ta);
                                    })
                                    .limit(3)
                                    .collect(Collectors.toList());
                        } else {
                            topBadges = badges.stream().limit(3).collect(Collectors.toList());
                        }
                    }
                    return LeaderboardResponse.LeaderboardEntry.builder()
                            .rank(rankCounter.getAndIncrement())
                            .userId(profile.getUserId())
                            .username(profile.getUsername())
                            .displayName(profile.getDisplayName())
                            .avatarUrl(profile.getAvatarUrl())
                            .level(stats.getCurrentLevel() != null ? stats.getCurrentLevel() : 1)
                            .xpThisWeek(getXpForTimeframe(stats, timeframe))
                            .recipesCooked(stats.getCompletionCount() != null ? stats.getCompletionCount() : 0L)
                            .streak(stats.getStreakCount() != null ? stats.getStreakCount() : 0)
                            .topBadges(topBadges)
                            .build();
                })
                .collect(Collectors.toList());

        // 5. Build response
        LeaderboardResponse.MyRank myRank = LeaderboardResponse.MyRank.builder()
                .rank(userRank)
                .xpThisWeek(userXp)
                .xpToNextRank(xpToNextRank)
                .nextRankPosition(nextRankPosition)
                .recipesCooked(userRecipesCooked)
                .build();

        return LeaderboardResponse.builder()
                .type(type)
                .timeframe(timeframe)
                .entries(entries)
                .myRank(myRank)
                .build();
    }

    /**
     * Get XP value based on timeframe.
     */
    private double getXpForTimeframe(Statistics stats, String timeframe) {
        if (stats == null) return 0.0;

        return switch (timeframe) {
            case "weekly" -> stats.getXpWeekly() != null ? stats.getXpWeekly() : 0.0;
            case "monthly" -> stats.getXpMonthly() != null ? stats.getXpMonthly() : 0.0;
            case "all_time" -> stats.getCurrentXP() != null ? stats.getCurrentXP() : 0.0;
            default -> stats.getXpWeekly() != null ? stats.getXpWeekly() : 0.0;
        };
    }

    /**
     * Sends gamification notification via Kafka to notification-service.
     * Fires for XP earned, level-ups, and badge achievements.
     */
    private void sendGamificationNotification(
            String userId, String displayName,
            double xpEarned, double totalXp,
            XpLevelResult levelResult, List<String> newBadges,
            String source, String recipeId, String sessionId) {
        
        try {
            boolean hasBadges = newBadges != null && !newBadges.isEmpty();
            if (xpEarned <= 0 && !levelResult.leveledUp() && !hasBadges) {
                log.debug("Skipping gamification notification - no XP, level-up, or badges for user {}", userId);
                return;
            }

            GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                    .userId(userId)
                    .displayName(displayName != null ? displayName : "Chef")
                    .xpEarned(xpEarned)
                    .totalXp(totalXp)
                    .leveledUp(levelResult.leveledUp())
                    .previousLevel(levelResult.previousLevel())
                    .newLevel(levelResult.newLevel())
                    .newTitle(levelResult.newTitle())
                    .newBadges(newBadges)
                    .source(source)
                    .recipeId(recipeId)
                    .sessionId(sessionId)
                    .build();

            kafkaTemplate.send(GAMIFICATION_TOPIC, event);
            log.info("Gamification notification sent: user={}, leveledUp={}, badges={}",
                    userId, levelResult.leveledUp(), newBadges);
        } catch (Exception e) {
            log.error("Failed to send gamification notification for user {}", userId, e);
        }
    }
}
