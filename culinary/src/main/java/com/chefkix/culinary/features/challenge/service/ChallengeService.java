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
    private final com.chefkix.culinary.common.helper.RecipeHelper recipeHelper;

    public ChallengeResponse getTodayChallenge(String userId) {
        // 1. Get today's Challenge from Pool
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // 2. Check if user has already completed it
        String todayStr = LocalDate.now(ZoneId.of("UTC")).toString(); // "2025-12-13"
        Optional<ChallengeLog> logOpt = challengeLogRepository.findByUserIdAndChallengeDate(userId, todayStr);

        boolean isCompleted = logOpt.isPresent();
        String completedAt = isCompleted && logOpt.get().getCompletedAt() != null
                ? logOpt.get().getCompletedAt().toString() : null;

        // 3. Find suggested recipes (Matching Recipes)
        // This is basic search logic based on Metadata
        List<ChallengeResponse.RecipePreviewDto> matchingRecipes = findMatchingRecipes(challenge.getCriteriaMetadata());

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        // Step 2: Add 1 day -> Get 00:00:00 of tomorrow
        // Example: Today is 12/13 -> Deadline is 12/14 at 00:00:00
        java.time.ZonedDateTime endOfDay = today.plusDays(1)
                .atStartOfDay(ZoneId.of("UTC"));

        // Step 3: Format to ISO 8601 standard (e.g., "2025-12-14T00:00:00Z")
        String endsAtStr = endOfDay.format(java.time.format.DateTimeFormatter.ISO_INSTANT);
        // 4. Map to response DTO
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
            .icon(extractChallengeIcon(challenge.getTitle()))
                .bonusXp(challenge.getBonusXp())
                .endsAt(endsAtStr)
                .criteria(challenge.getCriteriaMetadata()) // Return the JSON criteria object
                .completed(isCompleted)
                .completedAt(completedAt)
                .matchingRecipes(matchingRecipes) // Consider mapping to RecipePreviewDTO for a lighter response
                .build();
    }

    private String extractChallengeIcon(String title) {
        if (!StringUtils.hasText(title)) {
            return "🎯";
        }

        String[] tokens = title.trim().split("\\s+");
        if (tokens.length == 0) {
            return "🎯";
        }

        String lastToken = tokens[tokens.length - 1];
        boolean looksLikeEmoji = lastToken.codePoints().anyMatch(codePoint -> !Character.isLetterOrDigit(codePoint));
        return looksLikeEmoji ? lastToken : "🎯";
    }

    /**
     * Find suggested recipes based on Challenge Metadata
     */
    /**
     * Find suggested recipes
     */
    private List<ChallengeResponse.RecipePreviewDto> findMatchingRecipes(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            // Fallback: If no criteria, return 5 random recipes (or empty)
            return Collections.emptyList();
        }

        List<Recipe> recipes = new ArrayList<>();

        // 1. Prioritize search by Cuisine (more precise)
        if (criteria.containsKey("cuisineType")) {
            List<String> cuisines = getStringListCriteria(criteria, "cuisineType");
            // Call Repository
            recipes = recipeRepository.findTop5ByCuisineTypeInIgnoreCase(cuisines);
        }

        // 2. If no results found, try searching by Ingredients
        if (recipes.isEmpty() && criteria.containsKey("ingredientContains")) {
            List<String> ingredients = getStringListCriteria(criteria, "ingredientContains");
            // Call Repository
            recipes = recipeRepository.findTop5ByFullIngredientListInIgnoreCase(ingredients);
        }

        // 3. Fallback: If still empty (due to bad user input or no matches)
        // You can return an empty list or query findAll().stream().limit(3)... as desired.
        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Map from Entity (Recipe) to DTO (RecipePreviewDto)
        return recipes.stream()
                .map(this::mapToPreviewDto)
                .toList(); // Java 16+
        // .collect(Collectors.toList()); // If using older Java
    }

    private List<String> getStringListCriteria(Map<String, Object> criteria, String key) {
        Object rawValue = criteria.get(key);
        if (!(rawValue instanceof List<?> rawList)) {
            return Collections.emptyList();
        }

        return rawList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    /**
     * Logic: Automatic Completion based on Specs
     * @param userId User ID
     * @param recipe Recipe that was just cooked
     * @return Reward result (if any)
     */
    public Optional<ChallengeRewardResult> checkAndCompleteChallenge(String userId, Recipe recipe) {

        // 0. Get today's Challenge
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) return Optional.empty();

        String todayStr = LocalDate.now(ZoneId.of("UTC")).toString(); // "2025-12-13"

        // =================================================================
        // LOGIC 1: ALREADY COMPLETED CHECK (Duplicate Check)
        // =================================================================
        // "has the user already completed a challenge today?"
        boolean alreadyCompleted = challengeLogRepository
                .existsByUserIdAndChallengeDate(userId, todayStr);

        if (alreadyCompleted) {
            return Optional.empty(); // Already done -> Stop, no more rewards.
        }

        // =================================================================
        // LOGIC 2: RECIPE MATCH CHECK (Matching Check)
        // =================================================================
        // "check if the current session's recipeId matches a recipe in the challenge"
        // (Here we check against the challenge's Criteria instead of a hardcoded ID list for flexibility)
        if (challenge.isSatisfiedBy(recipe)) {

            // =============================================================
            // LOGIC 3: SAVE TO HISTORY (Save Log)
            // =============================================================
            // "if conditions are met... add to Challenge History"
            ChallengeLog historyLog = ChallengeLog.builder()
                    .userId(userId)
                    .challengeId(challenge.getId())
                    .challengeTitle(challenge.getTitle()) // Save snapshot of challenge title
                    .recipeId(recipe.getId())
                    .recipeTitle(recipe.getTitle())       // Save snapshot of recipe title
                    .challengeDate(todayStr)
                    .bonusXp(challenge.getBonusXp())
                    .completedAt(Instant.now())
                    .build();

            try {
                challengeLogRepository.save(historyLog);

                // Return result to notify Frontend
                return Optional.of(ChallengeRewardResult.builder()
                        .completed(true)
                        .bonusXp(challenge.getBonusXp())
                        .challengeTitle(challenge.getTitle())
                        .build());

            } catch (DuplicateKeyException e) {
                // Race condition: 2 parallel requests -> Only count 1
                return Optional.empty();
            }
        }

        // Conditions not met -> No reward
        return Optional.empty();
    }

    /**
     * Get paginated Challenge History
     * @param userId User ID
     * @param page Page number (starting from 0)
     * @param size Number of items per page (limit)
     */
    @Transactional(readOnly = true)
    public ChallengeHistoryResponse getChallengeHistory(String userId, int page, int size) {

        // 1. Query DB to get Page (to limit number of records returned)
        Pageable pageable = PageRequest.of(page, size, Sort.by("challengeDate").descending());
        Page<ChallengeLog> historyPage = challengeLogRepository.findByUserId(userId, pageable);

        // 2. Map Entity to DTO
        // Note: Result of this line is List<Dto>, NOT Page<Dto>
        List<ChallengeHistoryResponse.ChallengeItemDto> challengesList = historyPage.getContent() // <--- EXTRACT LIST
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

        // 4. Package Response (List and Stats only)
        return ChallengeHistoryResponse.builder()
                .challenges(challengesList) // Pass List in
                .stats(ChallengeHistoryResponse.StatsDto.builder()
                        .totalCompleted((long) allDateStrings.size())
                        .currentStreak(streakResult.getCurrentStreak())
                        .longestStreak(streakResult.getLongestStreak())
                        .totalBonusXp(totalBonusXp)
                        .build())
                .build();
    }

    /**
     * Helper: Convert Recipe Entity to lightweight DTO
     */
    private ChallengeResponse.RecipePreviewDto mapToPreviewDto(Recipe recipe) {
        return ChallengeResponse.RecipePreviewDto.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .xpReward(recipe.getXpReward()) // Your XP calculation function, or use the existing xpReward field
                .coverImageUrl(recipe.getCoverImageUrl()) // Fix getter to match your entity
                .totalTime(recipe.getTotalTimeMinutes())
                .difficulty(recipe.getDifficulty())
                .build();
    }

    // --- Helper Mapping (Fixed for ChallengeLog) ---
    private ChallengeHistoryResponse.ChallengeItemDto mapToDto(ChallengeLog log) {
        ChallengeHistoryResponse.RecipeShortInfo recipeInfo = null;

        if (StringUtils.hasText(log.getRecipeId())) {
            String recipeImageUrl = recipeRepository.findById(log.getRecipeId())
                    .map(Recipe::getCoverImageUrl)
                    .filter(images -> images != null && !images.isEmpty())
                    .map(images -> images.get(0))
                    .orElse(null);

            recipeInfo = ChallengeHistoryResponse.RecipeShortInfo.builder()
                    .id(log.getRecipeId())
                    .title(log.getRecipeTitle()) // ChallengeLog already has the title snapshot
                    .imageUrl(recipeImageUrl)
                    .build();
        }

        return ChallengeHistoryResponse.ChallengeItemDto.builder()
                .id(log.getChallengeId())
                .title(log.getChallengeTitle())
                .date(LocalDate.parse(log.getChallengeDate())) // Convert String back to LocalDate
                .completed(true) // If it's in the log, it is completed
                .completedAt(log.getCompletedAt() != null
                        ? LocalDateTime.ofInstant(log.getCompletedAt(), ZoneId.of("UTC")) : null)
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
        List<String> matchingRecipeIds = matchingRecipes.stream()
                .map(Recipe::getId).toList();

        // Count user's completed sessions this week with matching recipes
        // If no recipes match, progress is 0 (don't fallback to all recipes)
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
                    .map(log -> log.getCompletedAt() != null ? log.getCompletedAt().toString() : null)
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
                    .startsAt(ch.getStartsAt() != null ? ch.getStartsAt().toString() : null)
                    .endsAt(ch.getEndsAt() != null ? ch.getEndsAt().toString() : null)
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

                // Award XP to all participants
                Set<String> participantIds = communityChallengeRedisRepository.getParticipants(ch.getId());
                int bonusXp = ch.getRewardXpPerUser() > 0 ? ch.getRewardXpPerUser() : 50;
                for (String participantId : participantIds) {
                    recipeHelper.sendXpEvent(
                            participantId,
                            bonusXp,
                            "COMMUNITY_CHALLENGE",
                            null,
                            "Community challenge completed: " + ch.getTitle()
                    );
                }
                log.info("Awarded {} XP to {} participants for community challenge: {}",
                        bonusXp, participantIds.size(), ch.getTitle());
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
                    userCompletedAt = logOpt.get().getCompletedAt() != null
                            ? logOpt.get().getCompletedAt().toString() : null;
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
                    .startsAt(ch.getStartsAt() != null ? ch.getStartsAt().toString() : null)
                    .endsAt(ch.getEndsAt() != null ? ch.getEndsAt().toString() : null)
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

        boolean hasAnyCriteria = false;

        // Check cuisineType (must match if present)
        if (criteria.containsKey("cuisineType")) {
            hasAnyCriteria = true;
            @SuppressWarnings("unchecked")
            List<String> cuisines = (List<String>) criteria.get("cuisineType");
            if (recipe.getCuisineType() == null ||
                    cuisines.stream().noneMatch(c -> c.equalsIgnoreCase(recipe.getCuisineType()))) {
                return false;
            }
        }

        // Check skillTags (must match if present)
        if (criteria.containsKey("skillTags")) {
            hasAnyCriteria = true;
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) criteria.get("skillTags");
            if (recipe.getSkillTags() == null ||
                    recipe.getSkillTags().stream().noneMatch(t ->
                            tags.stream().anyMatch(ct -> ct.equalsIgnoreCase(t)))) {
                return false;
            }
        }

        // Check difficulty (must match if present)
        if (criteria.containsKey("difficulty")) {
            hasAnyCriteria = true;
            String difficulty = (String) criteria.get("difficulty");
            if (recipe.getDifficulty() == null ||
                    !recipe.getDifficulty().name().equalsIgnoreCase(difficulty)) {
                return false;
            }
        }

        return hasAnyCriteria;
    }
}