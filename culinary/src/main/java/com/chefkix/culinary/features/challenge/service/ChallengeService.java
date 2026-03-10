package com.chefkix.culinary.features.challenge.service;

import com.chefkix.culinary.features.challenge.dto.response.ChallengeHistoryResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeRewardResult;
import com.chefkix.culinary.features.challenge.dto.response.CommunityChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.SeasonalChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.WeeklyChallengeResponse;
import com.chefkix.culinary.features.challenge.entity.ChallengeLog;
import com.chefkix.culinary.features.challenge.entity.CommunityChallenge;
import com.chefkix.culinary.features.challenge.entity.SeasonalChallenge;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.helper.StreakCalculatorHelper;
import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.challenge.repository.CommunityChallengeRepository;
import com.chefkix.culinary.features.challenge.repository.CommunityChallengeRedisRepository;
import com.chefkix.culinary.features.challenge.repository.SeasonalChallengeRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeService {

    private final ChallengePoolService challengePoolService;
    private final ChallengeLogRepository challengeLogRepository;
    private final CommunityChallengeRepository communityChallengeRepository;
    private final CommunityChallengeRedisRepository communityChallengeRedisRepository;
    private final SeasonalChallengeRepository seasonalChallengeRepository;
    private final RecipeRepository recipeRepository;
    private final CookingSessionRepository cookingSessionRepository;
    private final StreakCalculatorHelper streakCalculator;

    public ChallengeResponse getTodayChallenge(String userId) {
        // 1. Lấy Challenge hôm nay từ Pool
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // 2. Kiểm tra User đã hoàn thành chưa
        String todayStr = LocalDate.now(ZoneId.of("UTC")).toString(); // "2025-12-13"
        Optional<ChallengeLog> logOpt = challengeLogRepository.findByUserIdAndChallengeDate(userId, todayStr);

        boolean isCompleted = logOpt.isPresent();
        String completedAt = isCompleted ? logOpt.get().getCompletedAt().toString() : null;

        // 3. Tìm món ăn gợi ý (Matching Recipes)
        // Đây là phần logic tìm kiếm cơ bản dựa trên Metadata
        List<ChallengeResponse.RecipePreviewDto> matchingRecipes = findMatchingRecipes(challenge.getCriteriaMetadata());

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        // B2: Cộng thêm 1 ngày -> Lấy 00:00:00 của ngày mai
        // Ví dụ: Hôm nay 13/12 -> Deadline là 14/12 lúc 00:00:00
        java.time.ZonedDateTime endOfDay = today.plusDays(1)
                .atStartOfDay(ZoneId.of("UTC"));

        // B3: Format sang chuẩn ISO 8601 (VD: "2025-12-14T00:00:00Z")
        String endsAtStr = endOfDay.format(java.time.format.DateTimeFormatter.ISO_INSTANT);
        // 4. Map sang DTO để trả về
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .bonusXp(challenge.getBonusXp())
                .endsAt(endsAtStr)
                .criteria(challenge.getCriteriaMetadata()) // Trả về cục JSON criteria
                .completed(isCompleted)
                .completedAt(completedAt)
                .matchingRecipes(matchingRecipes) // Bạn nên map sang RecipePreviewDTO để gọn hơn
                .build();
    }

    /**
     * Logic tìm món ăn gợi ý dựa trên Metadata của Challenge
     * (Đây là ví dụ đơn giản, bạn có thể viết query Mongo phức tạp hơn)
     */
    /**
     * Logic tìm món ăn gợi ý
     */
    private List<ChallengeResponse.RecipePreviewDto> findMatchingRecipes(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            // Fallback: Nếu không có tiêu chí, trả về 5 món bất kỳ (hoặc rỗng)
            return Collections.emptyList();
        }

        List<Recipe> recipes = new ArrayList<>();

        // 1. Ưu tiên tìm theo Cuisine (Vì nó chính xác hơn)
        if (criteria.containsKey("cuisineType")) {
            List<String> cuisines = (List<String>) criteria.get("cuisineType");
            // Gọi Repository
            recipes = recipeRepository.findTop5ByCuisineTypeInIgnoreCase(cuisines);
        }

        // 2. Nếu chưa tìm thấy gì, thử tìm theo Nguyên liệu
        if (recipes.isEmpty() && criteria.containsKey("fullI")) {
            List<String> ingredients = (List<String>) criteria.get("ingredientContains");
            // Gọi Repository
            recipes = recipeRepository.findTop5ByFullIngredientListInIgnoreCase(ingredients);
        }

        // 3. Fallback: Nếu vẫn rỗng (do user nhập liệu sai hoặc không khớp)
        // Bạn có thể trả về list rỗng hoặc query findAll().stream().limit(3)... tùy ý.
        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Map từ Entity (Recipe) sang DTO (RecipePreviewDto)
        return recipes.stream()
                .map(this::mapToPreviewDto)
                .toList(); // Java 16+
        // .collect(Collectors.toList()); // Nếu dùng Java cũ hơn
    }

    /**
     * Logic: Automatic Completion based on Specs
     * @param userId ID người dùng
     * @param recipe Món ăn vừa nấu xong
     * @return Kết quả thưởng (nếu có)
     */
    public Optional<ChallengeRewardResult> checkAndCompleteChallenge(String userId, Recipe recipe) {

        // 0. Lấy Challenge của hôm nay
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) return Optional.empty();

        String todayStr = LocalDate.now(ZoneId.of("UTC")).toString(); // "2025-12-13"

        // =================================================================
        // LOGIC 1: CHECK ĐÃ LÀM CHƯA? (Duplicate Check)
        // =================================================================
        // "hôm nay đã thực hiện 1 chellenge nào chưa"
        boolean alreadyCompleted = challengeLogRepository
                .existsByUserIdAndChallengeDate(userId, todayStr);

        if (alreadyCompleted) {
            return Optional.empty(); // Đã làm rồi -> Dừng lại, không thưởng nữa.
        }

        // =================================================================
        // LOGIC 2: CHECK RECIPE CÓ KHỚP KHÔNG? (Matching Check)
        // =================================================================
        // "check xem session đang thực hiện có recipeId khớp với recipe trong challenge"
        // (Ở đây ta check theo Criteria của challenge thay vì list ID cứng để linh hoạt hơn)
        if (challenge.isSatisfiedBy(recipe)) {

            // =============================================================
            // LOGIC 3: LƯU VÀO HISTORY (Save Log)
            // =============================================================
            // "nếu thỏa các điều kiện... thêm vào Challenge History"
            ChallengeLog historyLog = ChallengeLog.builder()
                    .userId(userId)
                    .challengeId(challenge.getId())
                    .challengeTitle(challenge.getTitle()) // Lưu Snapshot tên challenge
                    .recipeId(recipe.getId())
                    .recipeTitle(recipe.getTitle())       // Lưu Snapshot tên món ăn
                    .challengeDate(todayStr)
                    .bonusXp(challenge.getBonusXp())
                    .completedAt(Instant.now())
                    .build();

            try {
                challengeLogRepository.save(historyLog);

                // Trả về kết quả để báo cho Frontend
                return Optional.of(ChallengeRewardResult.builder()
                        .completed(true)
                        .bonusXp(challenge.getBonusXp())
                        .challengeTitle(challenge.getTitle())
                        .build());

            } catch (DuplicateKeyException e) {
                // Trường hợp 2 request chạy song song -> Chỉ tính 1 cái
                return Optional.empty();
            }
        }

        // Không khớp điều kiện -> Không thưởng
        return Optional.empty();
    }

    /**
     * Lấy lịch sử Challenge có phân trang
     * @param userId ID người dùng
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng item mỗi trang (limit)
     */
    @Transactional(readOnly = true)
    public ChallengeHistoryResponse getChallengeHistory(String userId, int page, int size) {

        // 1. Query DB lấy Page (để limit số lượng record trả về)
        Pageable pageable = PageRequest.of(page, size, Sort.by("challengeDate").descending());
        Page<ChallengeLog> historyPage = challengeLogRepository.findByUserId(userId, pageable);

        // 2. Map Entity sang DTO
        // Lưu ý: Kết quả của dòng này là List<Dto>, KHÔNG PHẢI Page<Dto>
        List<ChallengeHistoryResponse.ChallengeItemDto> challengesList = historyPage.getContent() // <--- BÓC LẤY LIST
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        List<String> allDateStrings = challengeLogRepository.findCompletedDatesByUserId(userId);

        // Convert List<String> -> List<LocalDate>
        List<LocalDate> allDates = allDateStrings.stream()
                .map(LocalDate::parse)
                .collect(Collectors.toList());

        var streakResult = streakCalculator.calculate(allDates);
        var sumResult = challengeLogRepository.sumBonusXpByUserId(userId);
        long totalBonusXp = (sumResult != null) ? sumResult.totalXp : 0;

        // 4. Đóng gói Response (Chỉ List và Stats)
        return ChallengeHistoryResponse.builder()
                .challenges(challengesList) // Truyền List vào
                .stats(ChallengeHistoryResponse.StatsDto.builder()
                        .totalCompleted((long) allDateStrings.size())
                        .currentStreak(streakResult.getCurrentStreak())
                        .longestStreak(streakResult.getLongestStreak())
                        .totalBonusXp(totalBonusXp)
                        .build())
                .build();
    }

    /**
     * Helper: Chuyển đổi Recipe Entity -> DTO gọn nhẹ
     */
    private ChallengeResponse.RecipePreviewDto mapToPreviewDto(Recipe recipe) {
        return ChallengeResponse.RecipePreviewDto.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .xpReward(recipe.getXpReward()) // Hàm tính XP của bạn, hoặc lấy field xpReward có sẵn
                .coverImageUrl(recipe.getCoverImageUrl()) // Sửa getter cho đúng với entity của bạn
                .totalTime(recipe.getTotalTimeMinutes())
                .difficulty(recipe.getDifficulty())
                .build();
    }

    // --- Helper Mapping (Fixed for ChallengeLog) ---
    private ChallengeHistoryResponse.ChallengeItemDto mapToDto(ChallengeLog log) {
        ChallengeHistoryResponse.RecipeShortInfo recipeInfo = null;

        if (StringUtils.hasText(log.getRecipeId())) {
            // Check cache or simple query
            recipeInfo = ChallengeHistoryResponse.RecipeShortInfo.builder()
                    .id(log.getRecipeId())
                    .title(log.getRecipeTitle()) // ChallengeLog already has the title snapshot
                    .build();
        }

        return ChallengeHistoryResponse.ChallengeItemDto.builder()
                .id(log.getChallengeId())
                .title(log.getChallengeTitle())
                .date(LocalDate.parse(log.getChallengeDate())) // Convert String back to LocalDate
                .completed(true) // If it's in the log, it is completed
                .completedAt(LocalDateTime.ofInstant(log.getCompletedAt(), ZoneId.of("UTC")))
                .bonusXpEarned(log.getBonusXp())
                .recipeCooked(recipeInfo)
                .build();
    }

    // ===============================================
    // WEEKLY CHALLENGES
    // ===============================================

    /**
     * Get current weekly challenge with progress for the user.
     * Progress = completed sessions this week matching the weekly criteria.
     */
    @Transactional(readOnly = true)
    public WeeklyChallengeResponse getWeeklyChallenge(String userId) {
        ChallengeDefinition weekly = challengePoolService.getThisWeekChallenge();
        if (weekly == null) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // Compute ISO week boundaries (Monday-Sunday)
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);

        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt = weekEnd.atStartOfDay();

        // Find matching recipes for this challenge criteria
        List<Recipe> matchingRecipes = findMatchingRecipes(weekly.getCriteriaMetadata()).stream()
                .map(dto -> recipeRepository.findById(dto.getId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        List<String> matchingRecipeIds = matchingRecipes.isEmpty()
                ? recipeRepository.findAll().stream().map(Recipe::getId).toList()
                : matchingRecipes.stream().map(Recipe::getId).toList();

        // Count user's completed sessions this week with matching recipes
        long progress = matchingRecipeIds.isEmpty() ? 0 :
                cookingSessionRepository.countByUserIdAndRecipeIdInAndStatusAndCompletedAtBetween(
                        userId, matchingRecipeIds, SessionStatus.COMPLETED, weekStartDt, weekEndDt);

        // Check if weekly is already marked completed (via ChallengeLog)
        String weekKey = String.format("WEEKLY-%d-W%02d", today.getYear(),
                today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        boolean isCompleted = challengeLogRepository.existsByUserIdAndChallengeDate(userId, weekKey);

        String completedAt = null;
        if (isCompleted) {
            completedAt = challengeLogRepository.findByUserIdAndChallengeDate(userId, weekKey)
                    .map(log -> log.getCompletedAt().toString())
                    .orElse(null);
        }

        // Matching recipe previews for FE display
        List<ChallengeResponse.RecipePreviewDto> previewDtos = findMatchingRecipes(weekly.getCriteriaMetadata());

        return WeeklyChallengeResponse.builder()
                .id(weekly.getId())
                .title(weekly.getTitle())
                .description(weekly.getDescription())
                .bonusXp(weekly.getBonusXp())
                .target(weekly.getTarget())
                .progress((int) Math.min(progress, weekly.getTarget()))
                .completed(isCompleted)
                .completedAt(completedAt)
                .startsAt(weekStart.atStartOfDay(ZoneId.of("UTC"))
                        .format(java.time.format.DateTimeFormatter.ISO_INSTANT))
                .endsAt(weekEnd.atStartOfDay(ZoneId.of("UTC"))
                        .format(java.time.format.DateTimeFormatter.ISO_INSTANT))
                .criteria(weekly.getCriteriaMetadata())
                .matchingRecipes(previewDtos)
                .build();
    }

    /**
     * Check if a recipe completion should advance/complete the weekly challenge.
     * Called during cooking session completion alongside daily challenge check.
     */
    public Optional<ChallengeRewardResult> checkAndCompleteWeeklyChallenge(String userId, Recipe recipe) {
        ChallengeDefinition weekly = challengePoolService.getThisWeekChallenge();
        if (weekly == null) return Optional.empty();

        // Check if recipe matches weekly criteria
        if (!weekly.isSatisfiedBy(recipe)) return Optional.empty();

        // Check if already completed
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        String weekKey = String.format("WEEKLY-%d-W%02d", today.getYear(),
                today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        if (challengeLogRepository.existsByUserIdAndChallengeDate(userId, weekKey)) {
            return Optional.empty(); // Already rewarded
        }

        // Compute current progress
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);

        // Count matching sessions (including the current one about to be saved)
        List<String> matchingRecipeIds = findRecipeIdsMatchingCriteria(weekly);
        long currentProgress = matchingRecipeIds.isEmpty() ? 0 :
                cookingSessionRepository.countByUserIdAndRecipeIdInAndStatusAndCompletedAtBetween(
                        userId, matchingRecipeIds, SessionStatus.COMPLETED,
                        weekStart.atStartOfDay(), weekEnd.atStartOfDay());

        // +1 for the current completion (not yet saved when this is called)
        long totalProgress = currentProgress + 1;

        if (totalProgress >= weekly.getTarget()) {
            // Weekly challenge completed!
            try {
                ChallengeLog log = ChallengeLog.builder()
                        .userId(userId)
                        .challengeId(weekly.getId())
                        .challengeTitle(weekly.getTitle())
                        .recipeId(recipe.getId())
                        .recipeTitle(recipe.getTitle())
                        .challengeDate(weekKey)
                        .bonusXp(weekly.getBonusXp())
                        .completedAt(Instant.now())
                        .build();
                challengeLogRepository.save(log);

                return Optional.of(ChallengeRewardResult.builder()
                        .completed(true)
                        .bonusXp(weekly.getBonusXp())
                        .challengeTitle(weekly.getTitle())
                        .build());
            } catch (DuplicateKeyException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private List<String> findRecipeIdsMatchingCriteria(ChallengeDefinition challenge) {
        // Use the same logic as findMatchingRecipes but return IDs only
        List<ChallengeResponse.RecipePreviewDto> previews = findMatchingRecipes(challenge.getCriteriaMetadata());
        return previews.stream().map(ChallengeResponse.RecipePreviewDto::getId).toList();
    }

    // ===============================================
    // COMMUNITY CHALLENGES
    // ===============================================

    /**
     * Get active community challenges with live progress from Redis.
     */
    @Transactional(readOnly = true)
    public List<CommunityChallengeResponse> getActiveCommunityChallenge(String userId) {
        List<CommunityChallenge> active = communityChallengeRepository
                .findByStatusAndEndsAtAfter("ACTIVE", Instant.now());

        return active.stream().map(ch -> {
            long progress = communityChallengeRedisRepository.getProgress(ch.getId());
            long participants = communityChallengeRedisRepository.getParticipantCount(ch.getId());
            boolean hasContributed = communityChallengeRedisRepository.isParticipant(ch.getId(), userId);
            double percent = ch.getTargetCount() > 0
                    ? Math.min(100.0, (progress * 100.0) / ch.getTargetCount())
                    : 0.0;

            return CommunityChallengeResponse.builder()
                    .id(ch.getId())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .emoji(ch.getEmoji())
                    .targetCount(ch.getTargetCount())
                    .targetUnit(ch.getTargetUnit())
                    .currentProgress(progress)
                    .participantCount(participants)
                    .progressPercent(Math.round(percent * 10.0) / 10.0)
                    .rewardXpPerUser(ch.getRewardXpPerUser())
                    .rewardBadgeId(ch.getRewardBadgeId())
                    .startsAt(ch.getStartsAt().toString())
                    .endsAt(ch.getEndsAt().toString())
                    .status(ch.getStatus())
                    .hasContributed(hasContributed)
                    .criteria(ch.getCriteria())
                    .tags(ch.getTags())
                    .build();
        }).toList();
    }

    /**
     * Check if a completed recipe should contribute to active community challenges.
     * Called during cooking session completion.
     */
    public void checkAndAdvanceCommunityChallenge(String userId, Recipe recipe) {
        List<CommunityChallenge> active = communityChallengeRepository
                .findByStatusAndEndsAtAfter("ACTIVE", Instant.now());

        for (CommunityChallenge ch : active) {
            if (!matchesCriteria(recipe, ch.getCriteria())) continue;

            // Increment progress and track participant
            long newProgress = communityChallengeRedisRepository.incrementProgress(ch.getId());
            communityChallengeRedisRepository.addParticipant(ch.getId(), userId);

            // Check if community goal reached
            if (newProgress >= ch.getTargetCount() && "ACTIVE".equals(ch.getStatus())) {
                ch.setStatus("COMPLETED");
                ch.setFinalProgress((int) newProgress);
                ch.setFinalParticipantCount((int)
                        communityChallengeRedisRepository.getParticipantCount(ch.getId()));
                communityChallengeRepository.save(ch);
                log.info("Community challenge completed: {} (progress: {}/{})",
                        ch.getTitle(), newProgress, ch.getTargetCount());
                // TODO: Kafka event to award XP to all participants
            }
        }
    }

    // ===============================================
    // SEASONAL CHALLENGES
    // ===============================================

    /**
     * Get active/upcoming seasonal challenges with per-user progress.
     */
    @Transactional(readOnly = true)
    public List<SeasonalChallengeResponse> getSeasonalChallenges(String userId) {
        List<SeasonalChallenge> challenges = seasonalChallengeRepository
                .findByStatusIn(List.of("ACTIVE", "UPCOMING"));

        return challenges.stream().map(ch -> {
            // Calculate user progress from ChallengeLog
            String seasonalKey = "SEASONAL-" + ch.getId();
            int userProgress = 0;
            boolean userCompleted = false;
            String userCompletedAt = null;

            if ("ACTIVE".equals(ch.getStatus())) {
                // Count user's qualifying completions during this event's date range
                userProgress = countUserSeasonalProgress(userId, ch);

                Optional<ChallengeLog> logOpt = challengeLogRepository
                        .findByUserIdAndChallengeDate(userId, seasonalKey);
                if (logOpt.isPresent()) {
                    userCompleted = true;
                    userCompletedAt = logOpt.get().getCompletedAt().toString();
                }
            }

            // Load featured recipes
            List<ChallengeResponse.RecipePreviewDto> featuredRecipes = List.of();
            if (ch.getFeaturedRecipeIds() != null && !ch.getFeaturedRecipeIds().isEmpty()) {
                featuredRecipes = recipeRepository.findAllById(ch.getFeaturedRecipeIds()).stream()
                        .map(this::mapToPreviewDto)
                        .toList();
            }

            return SeasonalChallengeResponse.builder()
                    .id(ch.getId())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .emoji(ch.getEmoji())
                    .theme(ch.getTheme())
                    .heroImageUrl(ch.getHeroImageUrl())
                    .accentColor(ch.getAccentColor())
                    .targetCount(ch.getTargetCount())
                    .targetUnit(ch.getTargetUnit())
                    .rewardXp(ch.getRewardXp())
                    .rewardBadgeId(ch.getRewardBadgeId())
                    .rewardBadgeName(ch.getRewardBadgeName())
                    .startsAt(ch.getStartsAt().toString())
                    .endsAt(ch.getEndsAt().toString())
                    .status(ch.getStatus())
                    .userProgress(Math.min(userProgress, ch.getTargetCount()))
                    .userCompleted(userCompleted)
                    .userCompletedAt(userCompletedAt)
                    .criteria(ch.getCriteria())
                    .featuredRecipes(featuredRecipes)
                    .tags(ch.getTags())
                    .build();
        }).toList();
    }

    /**
     * Check if a completed recipe should advance seasonal challenge progress.
     * Awards badge + XP when personal target is met.
     */
    public Optional<ChallengeRewardResult> checkAndAdvanceSeasonalChallenge(String userId, Recipe recipe) {
        List<SeasonalChallenge> active = seasonalChallengeRepository
                .findByStatusAndEndsAtAfter("ACTIVE", Instant.now());

        for (SeasonalChallenge ch : active) {
            if (!matchesCriteria(recipe, ch.getCriteria())) continue;

            String seasonalKey = "SEASONAL-" + ch.getId();

            // Already completed?
            if (challengeLogRepository.existsByUserIdAndChallengeDate(userId, seasonalKey)) continue;

            // Count progress (including this current completion)
            int progress = countUserSeasonalProgress(userId, ch) + 1;

            if (progress >= ch.getTargetCount()) {
                // Seasonal challenge completed!
                try {
                    ChallengeLog log = ChallengeLog.builder()
                            .userId(userId)
                            .challengeId(ch.getId())
                            .challengeTitle(ch.getTitle())
                            .recipeId(recipe.getId())
                            .recipeTitle(recipe.getTitle())
                            .challengeDate(seasonalKey)
                            .bonusXp(ch.getRewardXp())
                            .completedAt(Instant.now())
                            .build();
                    challengeLogRepository.save(log);

                    return Optional.of(ChallengeRewardResult.builder()
                            .completed(true)
                            .bonusXp(ch.getRewardXp())
                            .challengeTitle(ch.getTitle())
                            .build());
                } catch (DuplicateKeyException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Count how many qualifying recipes a user has completed during a seasonal event.
     */
    private int countUserSeasonalProgress(String userId, SeasonalChallenge ch) {
        LocalDateTime startDt = LocalDateTime.ofInstant(ch.getStartsAt(), ZoneId.of("UTC"));
        LocalDateTime endDt = LocalDateTime.ofInstant(ch.getEndsAt(), ZoneId.of("UTC"));

        // If the challenge has featured recipe IDs, use those; otherwise match by criteria
        List<String> recipeIds;
        if (ch.getFeaturedRecipeIds() != null && !ch.getFeaturedRecipeIds().isEmpty()) {
            recipeIds = ch.getFeaturedRecipeIds();
        } else {
            recipeIds = findMatchingRecipes(ch.getCriteria()).stream()
                    .map(ChallengeResponse.RecipePreviewDto::getId)
                    .toList();
        }

        if (recipeIds.isEmpty()) return 0;

        return (int) cookingSessionRepository.countByUserIdAndRecipeIdInAndStatusAndCompletedAtBetween(
                userId, recipeIds, SessionStatus.COMPLETED, startDt, endDt);
    }

    /**
     * Check if a recipe matches generic criteria map.
     * Used by community and seasonal challenges.
     */
    private boolean matchesCriteria(Recipe recipe, Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) return true; // No criteria = any recipe qualifies

        String type = (String) criteria.get("type");
        if ("COOK_ANY".equals(type)) return true;

        // Check cuisineType
        if (criteria.containsKey("cuisineType")) {
            @SuppressWarnings("unchecked")
            List<String> cuisines = (List<String>) criteria.get("cuisineType");
            if (recipe.getCuisineType() != null &&
                    cuisines.stream().anyMatch(c -> c.equalsIgnoreCase(recipe.getCuisineType()))) {
                return true;
            }
        }

        // Check skillTags
        if (criteria.containsKey("skillTags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) criteria.get("skillTags");
            if (recipe.getSkillTags() != null &&
                    recipe.getSkillTags().stream().anyMatch(t ->
                            tags.stream().anyMatch(ct -> ct.equalsIgnoreCase(t)))) {
                return true;
            }
        }

        // Check difficulty
        if (criteria.containsKey("difficulty")) {
            String difficulty = (String) criteria.get("difficulty");
            if (recipe.getDifficulty() != null &&
                    recipe.getDifficulty().name().equalsIgnoreCase(difficulty)) {
                return true;
            }
        }

        return false;
    }
}