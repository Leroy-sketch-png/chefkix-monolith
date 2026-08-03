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
import com.chefkix.culinary.features.challenge.model.ChallengeWeekKey;
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
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

String todayStr = LocalDate.now(ZoneId.of("UTC")).toString();
        Optional<ChallengeLog> logOpt = challengeLogRepository.findByUserIdAndChallengeDate(userId, todayStr);

        boolean isCompleted = logOpt.isPresent();
        String completedAt = isCompleted && logOpt.get().getCompletedAt() != null
                ? logOpt.get().getCompletedAt().toString() : null;

        List<ChallengeResponse.RecipePreviewDto> matchingRecipes = findMatchingRecipes(challenge.getCriteriaMetadata());

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        java.time.ZonedDateTime endOfDay = today.plusDays(1)
                .atStartOfDay(ZoneId.of("UTC"));

        String endsAtStr = endOfDay.format(java.time.format.DateTimeFormatter.ISO_INSTANT);
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
            .icon(extractChallengeIcon(challenge.getTitle()))
                .bonusXp(challenge.getBonusXp())
                .endsAt(endsAtStr)
.criteria(challenge.getCriteriaMetadata())
                .completed(isCompleted)
                .completedAt(completedAt)
.matchingRecipes(matchingRecipes)
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
     */
    /**
     */
    private List<ChallengeResponse.RecipePreviewDto> findMatchingRecipes(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return Collections.emptyList();
        }

        List<Recipe> recipes = new ArrayList<>();

        if (criteria.containsKey("cuisineType")) {
            List<String> cuisines = getStringListCriteria(criteria, "cuisineType");
            recipes = recipeRepository.findTop5ByCuisineTypeInIgnoreCase(cuisines);
        }

        if (recipes.isEmpty() && criteria.containsKey("ingredientContains")) {
            List<String> ingredients = getStringListCriteria(criteria, "ingredientContains");
            recipes = recipeRepository.findTop5ByFullIngredientListInIgnoreCase(ingredients);
        }

        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        return recipes.stream()
                .map(this::mapToPreviewDto)
.toList();
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
     */
    public Optional<ChallengeRewardResult> checkAndCompleteChallenge(String userId, Recipe recipe) {

        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) return Optional.empty();

String todayStr = LocalDate.now(ZoneId.of("UTC")).toString();

        boolean alreadyCompleted = challengeLogRepository
                .existsByUserIdAndChallengeDate(userId, todayStr);

        if (alreadyCompleted) {
return Optional.empty();
        }

        if (challenge.isSatisfiedBy(recipe)) {

            ChallengeLog historyLog = ChallengeLog.builder()
                    .userId(userId)
                    .challengeId(challenge.getId())
.challengeTitle(challenge.getTitle())
                    .recipeId(recipe.getId())
.recipeTitle(recipe.getTitle())
                    .challengeDate(todayStr)
                    .bonusXp(challenge.getBonusXp())
                    .completedAt(Instant.now())
                    .build();

            try {
                challengeLogRepository.save(historyLog);

                return Optional.of(ChallengeRewardResult.builder()
                        .completed(true)
                        .challengeKind("DAILY")
                        .challengeId(challenge.getId())
                        .bonusXp(challenge.getBonusXp())
                        .challengeTitle(challenge.getTitle())
                        .build());

            } catch (DuplicateKeyException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     */
    @Transactional(readOnly = true)
    public ChallengeHistoryResponse getChallengeHistory(String userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("challengeDate").descending());
        Page<ChallengeLog> historyPage = challengeLogRepository.findByUserId(userId, pageable);

List<ChallengeHistoryResponse.ChallengeItemDto> challengesList = historyPage.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        List<String> allDateStrings = challengeLogRepository.findCompletedDatesByUserId(userId);

        List<LocalDate> allDates = allDateStrings.stream()
                .map(LocalDate::parse)
                .collect(Collectors.toList());

        var streakResult = streakCalculator.calculate(allDates);
        var sumResult = challengeLogRepository.sumBonusXpByUserId(userId);
        long totalBonusXp = (sumResult != null) ? sumResult.totalXp : 0;

        return ChallengeHistoryResponse.builder()
.challenges(challengesList)
                .stats(ChallengeHistoryResponse.StatsDto.builder()
                        .totalCompleted((long) allDateStrings.size())
                        .currentStreak(streakResult.getCurrentStreak())
                        .longestStreak(streakResult.getLongestStreak())
                        .totalBonusXp(totalBonusXp)
                        .build())
                .build();
    }

    /**
     */
    private ChallengeResponse.RecipePreviewDto mapToPreviewDto(Recipe recipe) {
        return ChallengeResponse.RecipePreviewDto.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
.xpReward(recipe.getXpReward())
.coverImageUrl(recipe.getCoverImageUrl())
                .totalTime(recipe.getTotalTimeMinutes())
                .difficulty(recipe.getDifficulty())
                .build();
    }

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
.title(log.getRecipeTitle())
                    .imageUrl(recipeImageUrl)
                    .build();
        }

        return ChallengeHistoryResponse.ChallengeItemDto.builder()
                .id(log.getChallengeId())
                .title(log.getChallengeTitle())
.date(LocalDate.parse(log.getChallengeDate()))
.completed(true)
                .completedAt(log.getCompletedAt() != null
                        ? LocalDateTime.ofInstant(log.getCompletedAt(), ZoneId.of("UTC")) : null)
                .bonusXpEarned(log.getBonusXp())
                .recipeCooked(recipeInfo)
                .build();
    }


    /**
     */
    @Transactional(readOnly = true)
    public WeeklyChallengeResponse getWeeklyChallenge(String userId) {
        ChallengeDefinition weekly = challengePoolService.getThisWeekChallenge();
        if (weekly == null) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);

        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt = weekEnd.atStartOfDay();

        long progress = countQualifyingWeeklySessions(userId, weekly, weekStartDt, weekEndDt);

        String weekKey = ChallengeWeekKey.from(today);
        boolean isCompleted = challengeLogRepository.existsByUserIdAndChallengeDate(userId, weekKey);

        String completedAt = null;
        if (isCompleted) {
            completedAt = challengeLogRepository.findByUserIdAndChallengeDate(userId, weekKey)
                    .map(log -> log.getCompletedAt() != null ? log.getCompletedAt().toString() : null)
                    .orElse(null);
        }

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
     */
    public Optional<ChallengeRewardResult> checkAndCompleteWeeklyChallenge(String userId, Recipe recipe) {
        ChallengeDefinition weekly = challengePoolService.getThisWeekChallenge();
        if (weekly == null) return Optional.empty();

        if (!weekly.isSatisfiedBy(recipe)) return Optional.empty();

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        String weekKey = ChallengeWeekKey.from(today);
        if (challengeLogRepository.existsByUserIdAndChallengeDate(userId, weekKey)) {
return Optional.empty();
        }

        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);

        long currentProgress = countQualifyingWeeklySessions(
                userId, weekly, weekStart.atStartOfDay(), weekEnd.atStartOfDay());

        long totalProgress = currentProgress + 1;

        if (totalProgress >= weekly.getTarget()) {
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
                        .challengeKind("WEEKLY")
                        .challengeId(weekly.getId())
                        .bonusXp(weekly.getBonusXp())
                        .challengeTitle(weekly.getTitle())
                        .build());
            } catch (DuplicateKeyException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private long countQualifyingWeeklySessions(
            String userId,
            ChallengeDefinition challenge,
            LocalDateTime weekStart,
            LocalDateTime weekEnd) {
        var completedSessions = cookingSessionRepository.findByUserIdAndStatusAndCompletedAtBetween(
                userId, SessionStatus.COMPLETED, weekStart, weekEnd);
        if (completedSessions.isEmpty()) {
            return 0;
        }

        Set<String> recipeIds = completedSessions.stream()
                .map(session -> session.getRecipeId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> qualifyingRecipeIds = recipeRepository.findAllById(recipeIds).stream()
                .filter(challenge::isSatisfiedBy)
                .map(Recipe::getId)
                .collect(Collectors.toSet());

        return completedSessions.stream()
                .map(session -> session.getRecipeId())
                .filter(qualifyingRecipeIds::contains)
                .count();
    }


    /**
     */
    @Transactional(readOnly = true)
    public List<CommunityChallengeResponse> getActiveCommunityChallenge(String userId) {
        Instant now = Instant.now();
        List<CommunityChallenge> active = communityChallengeRepository
                .findByStatusAndEndsAtAfter("ACTIVE", now)
                .stream()
                .filter(ch -> ChallengeLifecyclePolicy.isActive(ch.getStartsAt(), ch.getEndsAt(), now))
                .toList();

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
     */
    public void checkAndAdvanceCommunityChallenge(String userId, Recipe recipe) {
        Instant now = Instant.now();
        List<CommunityChallenge> active = communityChallengeRepository
                .findByStatusAndEndsAtAfter("ACTIVE", now)
                .stream()
                .filter(ch -> ChallengeLifecyclePolicy.isActive(ch.getStartsAt(), ch.getEndsAt(), now))
                .toList();

        for (CommunityChallenge ch : active) {
            if (!matchesCriteria(recipe, ch.getCriteria())) continue;

            long newProgress = communityChallengeRedisRepository.incrementProgress(ch.getId());
            communityChallengeRedisRepository.addParticipant(ch.getId(), userId);

            if (newProgress >= ch.getTargetCount() && "ACTIVE".equals(ch.getStatus())) {
                ch.setStatus("COMPLETED");
                ch.setFinalProgress((int) newProgress);
                ch.setFinalParticipantCount((int)
                        communityChallengeRedisRepository.getParticipantCount(ch.getId()));
                communityChallengeRepository.save(ch);
                log.info("Community challenge completed: {} (progress: {}/{})",
                        ch.getTitle(), newProgress, ch.getTargetCount());

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


    /**
     */
    @Transactional(readOnly = true)
    public List<SeasonalChallengeResponse> getSeasonalChallenges(String userId) {
        Instant now = Instant.now();
        List<SeasonalChallenge> challenges = seasonalChallengeRepository
                .findByStatusIn(List.of("ACTIVE", "UPCOMING"));

        return challenges.stream()
                .filter(ch -> ChallengeLifecyclePolicy.statusAt(ch.getStartsAt(), ch.getEndsAt(), now)
                        != ChallengeLifecyclePolicy.WindowStatus.ENDED)
                .map(ch -> {
            ChallengeLifecyclePolicy.WindowStatus effectiveStatus =
                    ChallengeLifecyclePolicy.statusAt(ch.getStartsAt(), ch.getEndsAt(), now);
            String seasonalKey = "SEASONAL-" + ch.getId();
            int userProgress = 0;
            boolean userCompleted = false;
            String userCompletedAt = null;

            if (effectiveStatus == ChallengeLifecyclePolicy.WindowStatus.ACTIVE) {
                userProgress = countUserSeasonalProgress(userId, ch);

                Optional<ChallengeLog> logOpt = challengeLogRepository
                        .findByUserIdAndChallengeDate(userId, seasonalKey);
                if (logOpt.isPresent()) {
                    userCompleted = true;
                    userCompletedAt = logOpt.get().getCompletedAt() != null
                            ? logOpt.get().getCompletedAt().toString() : null;
                }
            }

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
                    .status(effectiveStatus.name())
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
     */
    public Optional<ChallengeRewardResult> checkAndAdvanceSeasonalChallenge(String userId, Recipe recipe) {
        Instant now = Instant.now();
        List<SeasonalChallenge> active = seasonalChallengeRepository
                .findByStatusIn(List.of("ACTIVE", "UPCOMING"))
                .stream()
                .filter(ch -> ChallengeLifecyclePolicy.isActive(ch.getStartsAt(), ch.getEndsAt(), now))
                .toList();

        for (SeasonalChallenge ch : active) {
            if (!matchesCriteria(recipe, ch.getCriteria())) continue;

            String seasonalKey = "SEASONAL-" + ch.getId();

            if (challengeLogRepository.existsByUserIdAndChallengeDate(userId, seasonalKey)) continue;

            int progress = countUserSeasonalProgress(userId, ch) + 1;

            if (progress >= ch.getTargetCount()) {
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
                            .challengeKind("SEASONAL")
                            .challengeId(ch.getId())
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
     */
    private int countUserSeasonalProgress(String userId, SeasonalChallenge ch) {
        LocalDateTime startDt = LocalDateTime.ofInstant(ch.getStartsAt(), ZoneId.of("UTC"));
        LocalDateTime endDt = LocalDateTime.ofInstant(ch.getEndsAt(), ZoneId.of("UTC"));

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
     */
    private boolean matchesCriteria(Recipe recipe, Map<String, Object> criteria) {
if (criteria == null || criteria.isEmpty()) return true;

        String type = (String) criteria.get("type");
        if ("COOK_ANY".equals(type)) return true;

        boolean hasAnyCriteria = false;

        if (criteria.containsKey("cuisineType")) {
            hasAnyCriteria = true;
            @SuppressWarnings("unchecked")
            List<String> cuisines = (List<String>) criteria.get("cuisineType");
            if (recipe.getCuisineType() == null ||
                    cuisines.stream().noneMatch(c -> c.equalsIgnoreCase(recipe.getCuisineType()))) {
                return false;
            }
        }

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
