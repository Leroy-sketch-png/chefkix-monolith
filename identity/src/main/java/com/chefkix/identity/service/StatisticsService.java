package com.chefkix.identity.service;

import com.chefkix.shared.event.GamificationNotificationEvent;
import com.chefkix.shared.event.ReminderEvent;
import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.identity.dto.response.LeaderboardResponse;
import com.chefkix.identity.dto.response.CreatorStatsResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.dto.response.internal.InternalCreatorInsightsResponse;
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
import java.util.HashSet;
import java.util.List;
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
public class StatisticsService {

  private static final String GAMIFICATION_TOPIC = "gamification-delivery";
  private static final String REMINDER_TOPIC = "reminder-delivery";

  MongoTemplate mongoTemplate;
  UserProfileRepository userProfileRepository;
  ProfileMapper profileMapper;
  KafkaTemplate<String, Object> kafkaTemplate;

  @Lazy RecipeProvider recipeProvider;

  /**
   * Hàm chính xử lý logic sau khi hoàn thành Recipe (Được gọi từ Recipe Service). Thực hiện: Cộng
   * XP + Check Level + Thêm Badges + Tăng Counter -> Save 1 lần.
   */
  @Transactional
  @Retryable(
      retryFor = {OptimisticLockingFailureException.class},
      maxAttempts = 5)
  public RecipeCompletionResponse updateAfterCompletion(InternalCompletionRequest request) {
    // Internal method called by culinary module via ProfileProvider.
    // Prefer request.userId over SecurityContext since auth may be null for internal calls.
    String userId = request.getUserId();
    if (userId == null || userId.isEmpty()) {
      // Fallback to SecurityContext only if userId not provided in request
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null) {
        userId = auth.getName();
      } else {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
    }

    // 1. Lấy Profile
    UserProfile profile =
        userProfileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    Statistics stats =
        (profile.getStatistics() != null) ? profile.getStatistics() : Statistics.builder().build();

    // 2. XỬ LÝ XP & LEVEL (Dùng hàm helper private) - capture result for level-up detection
    XpLevelResult levelResult = applyXpAndLevelLogic(stats, request.getXpAmount());

    // 3. XỬ LÝ BADGES (Logic thêm mới và tránh trùng lặp)
    List<String> actuallyAddedBadges = new ArrayList<>();
    if (request.getNewBadges() != null && !request.getNewBadges().isEmpty()) {
      // Khởi tạo list badge nếu chưa có
      if (stats.getBadges() == null) {
        stats.setBadges(new ArrayList<>());
      }
      if (stats.getBadgeTimestamps() == null) {
        stats.setBadgeTimestamps(new java.util.HashMap<>());
      }

      for (String badge : request.getNewBadges()) {
        // Chỉ thêm nếu user chưa có badge này
        if (!stats.getBadges().contains(badge)) {
          stats.getBadges().add(badge);
          stats.getBadgeTimestamps().putIfAbsent(badge, java.time.Instant.now());
          actuallyAddedBadges.add(badge);
        }
      }
    }

    // 4. INCREMENT COOKING COMPLETION COUNTER
    // completionCount = cooking sessions completed (distinct from totalRecipesPublished which
    // is tracked via Kafka PostCreatedEvent/PostDeletedEvent in PostEventListener).
    long currentCount = stats.getCompletionCount() == null ? 0L : stats.getCompletionCount();
    stats.setCompletionCount(currentCount + 1);

    // 5. CẬP NHẬT VÀ LƯU (1 lần duy nhất)
    profile.setStatistics(stats);
    userProfileRepository.save(profile);

    log.info(
        "Recipe Completed for User {}: +{} XP, New Level: {}, Added Badges: {}",
        userId,
        request.getXpAmount(),
        stats.getCurrentLevel(),
        actuallyAddedBadges);

    // 6. MAP RA DTO RIÊNG CHO RECIPE SERVICE (including level-up info for frontend celebration)
    int xpToNextLevel = (int) (stats.getCurrentXPGoal() - stats.getCurrentXP());
    return RecipeCompletionResponse.builder()
        .userId(userId)
        .currentXP(stats.getCurrentXP())
        .currentXPGoal(stats.getCurrentXPGoal())
        .currentLevel(stats.getCurrentLevel())
        .completionCount(stats.getCompletionCount())
        .leveledUp(levelResult.leveledUp())
        .oldLevel(levelResult.previousLevel())
        .newLevel(levelResult.newLevel())
        .xpToNextLevel(xpToNextLevel)
        .build();
  }

  /** Hàm thêm XP thủ công (Admin hoặc sự kiện khác) */
  @Transactional
  @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
  public ProfileResponse addXp(String userId, double xpAmount) {
      Statistics stats = processXpAndStatsUpdate(userId, xpAmount, null, false);

      // Fetch lại profile để map ra response
      UserProfile profile = userProfileRepository.findByUserId(userId).orElseThrow();
      return profileMapper.toProfileResponse(profile);
  }

  // --- PRIVATE HELPER METHODS (Tối ưu tái sử dụng) ---

  /**
   * Result of XP and level calculation, used for notification triggering.
   */
  private record XpLevelResult(boolean leveledUp, int previousLevel, int newLevel, String newTitle) {}

  /**
   * Logic cốt lõi để tính toán XP dư và Level up Hàm này chỉ thay đổi object Statistics, không gọi
   * DB.
   * 
   * @return XpLevelResult with level change info for notifications
   */
  private XpLevelResult applyXpAndLevelLogic(Statistics stats, double xpAmount) {
    int previousLevel = stats.getCurrentLevel();
    stats.setCurrentXP(stats.getCurrentXP() + xpAmount);

    // Track weekly and monthly XP for leaderboard
    double currentWeeklyXp = stats.getXpWeekly() != null ? stats.getXpWeekly() : 0.0;
    double currentMonthlyXp = stats.getXpMonthly() != null ? stats.getXpMonthly() : 0.0;
    stats.setXpWeekly(currentWeeklyXp + xpAmount);
    stats.setXpMonthly(currentMonthlyXp + xpAmount);

    boolean leveledUp = false;

    // Vòng lặp check lên cấp (có thể lên nhiều cấp 1 lúc)
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

  private Title getTitleForLevel(int level) {
    if (level >= 40) return Title.SEMIPRO;
    if (level >= 20) return Title.AMATEUR;
    return Title.BEGINNER;
  }

  private double calculateNewXpGoal(int currentLevel) {
    return (int) (1000 * Math.pow(1.1, currentLevel));
  }

  public void incrementCounter(String userId, String fieldName, int amount) {
    Query query = Query.query(Criteria.where("userId").is(userId));
    Update update = new Update().inc("statistics." + fieldName, amount);
    mongoTemplate.updateFirst(query, update, "user_profiles");
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
        log.info("Kafka Event: Processing reward {} XP for user {} with badges {}, challengeCompleted={}",
                amount, userId, badges, challengeCompleted);

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
                stats.setBadgeTimestamps(new java.util.HashMap<>());
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

        // Apply 72-hour cooking streak logic (per spec: "any 72-hour rolling window")
        Instant now = Instant.now();
        Instant lastCook = stats.getLastCookAt();

        if (lastCook == null) {
            // First cook ever - start streak at 1
            stats.setStreakCount(1);
            log.info("User {} started streak: 1 (first cook)", userId);
        } else {
            Duration sinceLastCook = Duration.between(lastCook, now);
            long hoursSinceLastCook = sinceLastCook.toHours();

            if (hoursSinceLastCook <= 72) {
                // Within 72-hour window - increment streak
                int newStreak = (stats.getStreakCount() != null ? stats.getStreakCount() : 0) + 1;
                stats.setStreakCount(newStreak);
                log.info("User {} streak continued: {} ({}h since last cook)", userId, newStreak, hoursSinceLastCook);
            } else {
                // More than 72 hours - streak broken, reset to 1
                stats.setStreakCount(1);
                log.info("User {} streak reset to 1 ({}h since last cook, exceeded 72h)", userId, hoursSinceLastCook);
            }
        }

        // Update lastCookAt timestamp
        stats.setLastCookAt(now);

        // Increment completion count
        long currentCount = stats.getCompletionCount() == null ? 0L : stats.getCompletionCount();
        stats.setCompletionCount(currentCount + 1);

        // Handle challenge streak (separate from cooking streak, uses 24-hour window)
        if (challengeCompleted) {
            Instant lastChallenge = stats.getLastChallengeAt();

            if (lastChallenge == null) {
                // First challenge ever
                stats.setChallengeStreak(1);
                log.info("User {} started challenge streak: 1 (first challenge)", userId);
            } else {
                Duration sinceLastChallenge = Duration.between(lastChallenge, now);
                long hoursSinceLastChallenge = sinceLastChallenge.toHours();

                // Challenge streak uses 24-48 hour window (must complete roughly daily)
                // More than 48 hours means you missed a day
                if (hoursSinceLastChallenge <= 48) {
                    int newChallengeStreak = (stats.getChallengeStreak() != null ? stats.getChallengeStreak() : 0) + 1;
                    stats.setChallengeStreak(newChallengeStreak);
                    log.info("User {} challenge streak continued: {} ({}h since last)", userId, newChallengeStreak, hoursSinceLastChallenge);
                } else {
                    stats.setChallengeStreak(1);
                    log.info("User {} challenge streak reset to 1 ({}h since last, exceeded 48h)", userId, hoursSinceLastChallenge);
                }
            }
            stats.setLastChallengeAt(now);
        }

        // Save
        profile.setStatistics(stats);
        userProfileRepository.save(profile);

        // Send gamification notification (XP, level-up, badges)
        sendGamificationNotification(
                userId, profile.getDisplayName(),
                amount, stats.getCurrentXP(),
                levelResult, actuallyNewBadges,
                "COOKING_SESSION", null, null);
    }

    private Statistics processXpAndStatsUpdate(String userId, double xpAmount, List<String> newBadges, boolean incrementRecipeCount) {
        // A. Lấy Profile (Tự tạo nếu chưa có - Fail safe)
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Statistics stats = (profile.getStatistics() != null) ? profile.getStatistics() : Statistics.builder().build();

        // B. XỬ LÝ XP & LEVEL (Logic cốt lõi - Đã sửa ở dưới)
        applyXpAndLevelLogic(stats, xpAmount);

        // C. XỬ LÝ BADGES (Dùng Set để code gọn và không trùng)
        if (newBadges != null && !newBadges.isEmpty()) {
            Set<String> currentBadges = new HashSet<>(stats.getBadges() != null ? stats.getBadges() : new ArrayList<>());
            if (stats.getBadgeTimestamps() == null) {
                stats.setBadgeTimestamps(new java.util.HashMap<>());
            }
            for (String badge : newBadges) {
                if (!currentBadges.contains(badge)) {
                    stats.getBadgeTimestamps().putIfAbsent(badge, java.time.Instant.now());
                }
            }
            currentBadges.addAll(newBadges);
            stats.setBadges(new ArrayList<>(currentBadges));
        }

        // D. XỬ LÝ COUNTER (Số món đã nấu)
        if (incrementRecipeCount) {
            long currentCount = stats.getCompletionCount() == null ? 0L : stats.getCompletionCount();
            stats.setCompletionCount(currentCount + 1);
        }

        // E. LƯU DATABASE (MongoDB)
        profile.setStatistics(stats);
        userProfileRepository.save(profile);

        return stats;
    }

    /**
     * Hàm xử lý phần thưởng cho Creator (được gọi khi có người khác nấu recipe của họ).
     * Logic:
     * 1. Tracking: Tăng xpEarnedAsCreator & totalCooks (để xem stats).
     * 2. Gamification: Tăng currentXP & Tính Level (để user lên cấp).
     */
    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void applyCreatorReward(String creatorId, double xpAmount) {
        log.info("Applying Creator Reward for User {}: +{} XP", creatorId, xpAmount);

        // 1. Lấy Profile (Fail-safe: tạo mới nếu chưa có)
        UserProfile profile = userProfileRepository.findByUserId(creatorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Statistics stats = (profile.getStatistics() != null)
                ? profile.getStatistics()
                : Statistics.builder().build();

        // --- PHẦN 1: TRACKING (Chỉ để thống kê, không tính level) ---
        // Cộng dồn XP kiếm được từ nghề Creator
        long creatorXpToAdd = (long) xpAmount;
        long currentCreatorXp = stats.getXpEarnedAsCreator() == null ? 0L : stats.getXpEarnedAsCreator();
        stats.setXpEarnedAsCreator(currentCreatorXp + creatorXpToAdd);

        // Tăng số lượt người khác đã nấu món của mình
        long currentTotalCooks = stats.getTotalCooksOfYourRecipes() == null ? 0L : stats.getTotalCooksOfYourRecipes();
        stats.setTotalCooksOfYourRecipes(currentTotalCooks + 1);

        // Nếu bạn có làm tracking theo tuần (như đã bàn ở các câu trước)
        stats.setWeeklyCreatorCooks(stats.getWeeklyCreatorCooks() + 1);
        stats.setWeeklyCreatorXp(stats.getWeeklyCreatorXp() + creatorXpToAdd);


        // --- PHẦN 2: GAMIFICATION (Tác động trực tiếp đến Level user) ---
        // Tái sử dụng hàm helper private bạn đã viết để cộng currentXP và tính Level Up
        // Hàm này sẽ tự động: currentXP += xpAmount và check while (currentXP >= goal)
        XpLevelResult levelResult = applyXpAndLevelLogic(stats, xpAmount);


        // --- LƯU DATABASE ---
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

    public CreatorStatsResponse getMyCreatorStats(String userId) {
        // 1. LẤY SỐ LIỆU TỔNG (Từ Local DB - Cực nhanh)
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

        // 3. TÍNH TOÁN BADGES
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

        // Badge: Prolific Creator (Dựa trên tổng bài - Local Data)
        if (totalRecipes >= 10) {
            badges.add(CreatorStatsResponse.CreatorBadgeDto.builder()
                    .name("Prolific Creator")
                    .icon("✍️")
                    .recipeTitle(null) // Badge cho người, không phải cho bài
                    .build());
        }

        // Badge: Dựa trên từng bài (Remote Data)
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
        log.info("Fetching {} {} leaderboard (limit: {}) for user {}", type, timeframe, limit, currentUserId);

        // 1. Get all user profiles (for global) or filter by friends
        List<UserProfile> profiles;
        if ("friends".equals(type) && friendIds != null && !friendIds.isEmpty()) {
            profiles = userProfileRepository.findAllById(friendIds)
                    .stream()
                    .filter(p -> p.getStatistics() != null)
                    .collect(Collectors.toList());
            // Add current user to friends list for comparison
            userProfileRepository.findByUserId(currentUserId).ifPresent(profiles::add);
        } else {
            // Global leaderboard - all users with stats
            profiles = userProfileRepository.findAll()
                    .stream()
                    .filter(p -> p.getStatistics() != null)
                    .collect(Collectors.toList());
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
            // Only send if there's something meaningful to notify
            if (!levelResult.leveledUp() && (newBadges == null || newBadges.isEmpty())) {
                // Just XP earned without level-up or badges - skip notification to avoid spam
                log.debug("Skipping gamification notification - no level-up or badges for user {}", userId);
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
