package com.chefkix.culinary.features.achievement.service;

import com.chefkix.culinary.features.achievement.dto.SkillTreeResponse;
import com.chefkix.culinary.features.achievement.entity.Achievement;
import com.chefkix.culinary.features.achievement.entity.AchievementCategory;
import com.chefkix.culinary.features.achievement.entity.CriteriaType;
import com.chefkix.culinary.features.achievement.entity.UserAchievement;
import com.chefkix.culinary.features.achievement.repository.AchievementRepository;
import com.chefkix.culinary.features.achievement.repository.UserAchievementRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.identity.api.ProfileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final CookingSessionRepository cookingSessionRepository;
    private final RecipeRepository recipeRepository;
    private final ProfileProvider profileProvider;

    private volatile List<Achievement> cachedAchievements;
    private volatile long cacheTimestamp;
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    private List<Achievement> getCachedAchievements() {
        if (cachedAchievements == null || System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS) {
            cachedAchievements = achievementRepository.findAll();
            cacheTimestamp = System.currentTimeMillis();
        }
        return cachedAchievements;
    }

    /**
     */
    public SkillTreeResponse getSkillTree(String userId) {
        List<Achievement> allAchievements = getCachedAchievements();
        List<UserAchievement> userProgress = userAchievementRepository.findByUserId(userId);

        Map<String, UserAchievement> progressMap = userProgress.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementCode, ua -> ua, (a, b) -> a));

        Set<String> unlockedCodes = userProgress.stream()
                .filter(UserAchievement::isUnlocked)
                .map(UserAchievement::getAchievementCode)
                .collect(Collectors.toSet());

        Map<String, List<Achievement>> pathGroups = allAchievements.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getPathId() != null ? a.getPathId() : "general",
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SkillTreeResponse.SkillPath> paths = new ArrayList<>();
        int totalUnlocked = 0;

        for (Map.Entry<String, List<Achievement>> entry : pathGroups.entrySet()) {
            String pathId = entry.getKey();
            List<Achievement> pathAchievements = entry.getValue();
            pathAchievements.sort(Comparator.comparingInt(Achievement::getTier));

            List<SkillTreeResponse.AchievementNode> nodes = new ArrayList<>();
            int pathUnlocked = 0;

            for (Achievement a : pathAchievements) {
                UserAchievement ua = progressMap.get(a.getCode());
                boolean unlocked = ua != null && ua.isUnlocked();
                if (unlocked) pathUnlocked++;

                boolean prerequisiteMet = a.getPrerequisiteCode() == null
                        || unlockedCodes.contains(a.getPrerequisiteCode());

                boolean isVisible = !a.isHidden() || unlocked || prerequisiteMet;

                nodes.add(SkillTreeResponse.AchievementNode.builder()
                        .code(a.getCode())
                        .name(isVisible ? a.getName() : "???")
                        .description(isVisible ? a.getDescription() : "Hidden achievement")
.icon(isVisible ? a.getIcon() : "\uD83D\uDD12")
                        .tier(a.getTier())
                        .category(a.getCategory())
                        .hidden(a.isHidden())
                        .premium(a.isPremium())
                        .currentProgress(ua != null ? ua.getCurrentProgress() : 0)
                        .requiredProgress(a.getCriteriaThreshold())
                        .unlocked(unlocked)
                        .unlockedAt(ua != null ? ua.getUnlockedAt() : null)
                        .prerequisiteCode(a.getPrerequisiteCode())
                        .prerequisiteMet(prerequisiteMet)
                        .build());
            }

            totalUnlocked += pathUnlocked;
            AchievementCategory category = pathAchievements.isEmpty() ? AchievementCategory.CUISINE
                    : pathAchievements.get(0).getCategory();

            paths.add(SkillTreeResponse.SkillPath.builder()
                    .pathId(pathId)
                    .pathName(derivePathName(pathId))
                    .category(category)
                    .nodes(nodes)
                    .unlockedCount(pathUnlocked)
                    .totalCount(pathAchievements.size())
                    .build());
        }

        return SkillTreeResponse.builder()
                .paths(paths)
                .totalUnlocked(totalUnlocked)
                .totalAchievements(allAchievements.size())
                .build();
    }

    /**
     */
    public List<Achievement> getAllAchievements() {
        return getCachedAchievements();
    }

    /**
     */
    public List<UserAchievement> getUnlockedAchievements(String userId) {
        return userAchievementRepository.findByUserIdAndUnlocked(userId, true);
    }

    /**
     */
    public List<String> evaluateAfterCookingCompletion(String userId, CookingSession session, Recipe recipe) {
        List<String> newlyUnlocked = new ArrayList<>();

        try {
            newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.TOTAL_COOKS, null));

            if (recipe.getCuisineType() != null && !recipe.getCuisineType().isBlank()) {
                newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.COOK_CUISINE_COUNT, recipe.getCuisineType()));
            }

            if (recipe.getSkillTags() != null) {
                for (String technique : recipe.getSkillTags()) {
                    newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.USE_TECHNIQUE_COUNT, technique));
                }
            }

            evaluateStreakAchievements(userId, newlyUnlocked);

            if (session.getStartedAt() != null) {
                int hour = session.getStartedAt().atOffset(ZoneOffset.UTC).getHour();
                if (hour >= 0 && hour < 5) {
                    newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.COOK_AFTER_MIDNIGHT, null));
                }
            }

            if (session.getCompletedAt() != null && session.getStartedAt() != null && recipe.getTotalTimeMinutes() > 0) {
                long actualMinutes = java.time.Duration.between(session.getStartedAt(), session.getCompletedAt()).toMinutes();
                if (actualMinutes < recipe.getTotalTimeMinutes()) {
                    newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.BEAT_ESTIMATED_TIME, null));
                }
            }

            if (!newlyUnlocked.isEmpty()) {
                log.info("User {} unlocked {} achievements after cooking: {}", userId, newlyUnlocked.size(), newlyUnlocked);
            }
        } catch (Exception e) {
            log.error("Achievement evaluation failed for user {}: {}", userId, e.getMessage(), e);
        }

        return newlyUnlocked;
    }

    /**
     */
    public List<String> evaluateSocialAchievements(String userId) {
        List<String> newlyUnlocked = new ArrayList<>();
        try {
            newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.FOLLOWERS_COUNT, null));
            newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.LIKES_RECEIVED, null));
            newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.RECIPES_PUBLISHED, null));
            newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.OTHERS_COOKED_YOUR_RECIPES, null));
        } catch (Exception e) {
            log.error("Social achievement evaluation failed for user {}: {}", userId, e.getMessage(), e);
        }
        return newlyUnlocked;
    }


    /**
     */
    private List<String> evaluateCriteria(String userId, CriteriaType criteriaType, String target) {
        List<Achievement> matching = (target != null)
                ? achievementRepository.findByCriteriaTypeAndCriteriaTarget(criteriaType, target)
                : achievementRepository.findByCriteriaType(criteriaType);

        if (matching.isEmpty()) return List.of();

        int currentCount = computeCurrentCount(userId, criteriaType, target);
        List<String> newlyUnlocked = new ArrayList<>();

        Map<String, UserAchievement> userAchievementMap = userAchievementRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementCode, ua -> ua, (a, b) -> a));

        for (Achievement achievement : matching) {
            UserAchievement ua = userAchievementMap.getOrDefault(achievement.getCode(),
                    UserAchievement.builder()
                            .userId(userId)
                            .achievementCode(achievement.getCode())
                            .requiredProgress(achievement.getCriteriaThreshold())
                            .currentProgress(0)
                            .unlocked(false)
                            .build());

            if (ua.isUnlocked()) continue;

            if (achievement.getPrerequisiteCode() != null) {
                UserAchievement prereq = userAchievementMap.get(achievement.getPrerequisiteCode());
                boolean prereqUnlocked = prereq != null && prereq.isUnlocked();
                if (!prereqUnlocked) continue;
            }

            ua.setCurrentProgress(Math.min(currentCount, achievement.getCriteriaThreshold()));

            if (currentCount >= achievement.getCriteriaThreshold()) {
                ua.setUnlocked(true);
                ua.setUnlockedAt(Instant.now());
                newlyUnlocked.add(achievement.getCode());
            }

            userAchievementRepository.save(ua);
        }

        return newlyUnlocked;
    }

    /**
     */
    private int computeCurrentCount(String userId, CriteriaType type, String target) {
        return switch (type) {
            case TOTAL_COOKS -> (int) countAllCompletedSessions(userId);

            case COOK_CUISINE_COUNT -> countSessionsByCuisine(userId, target);

            case USE_TECHNIQUE_COUNT -> countSessionsByTechnique(userId, target);

            case STREAK_DAYS -> getStreakDays(userId);

            case COOK_AFTER_MIDNIGHT -> countMidnightCooks(userId);

            case BEAT_ESTIMATED_TIME -> countBeatEstimatedTime(userId);

            case RECIPES_PUBLISHED -> (int) recipeRepository
                    .countByUserIdAndStatus(userId, com.chefkix.culinary.common.enums.RecipeStatus.PUBLISHED);

            case FOLLOWERS_COUNT -> getFollowerCount(userId);

            case LIKES_RECEIVED -> getTotalLikesReceived(userId);

            case OTHERS_COOKED_YOUR_RECIPES -> countOthersCookedYourRecipes(userId);
        };
    }


    private long countAllCompletedSessions(String userId) {
        return cookingSessionRepository
                .findAllByUserIdAndStatusIn(userId,
                List.of(SessionStatus.COMPLETED, SessionStatus.POSTED, SessionStatus.POST_DELETED),
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .getTotalElements();
    }

    private int countSessionsByCuisine(String userId, String cuisineType) {
        List<CookingSession> sessions = cookingSessionRepository
                .findTop20ByUserIdOrderByStartedAtDesc(userId);

        long totalCompleted = countAllCompletedSessions(userId);
        if (totalCompleted <= 20) {
            Set<String> recipeIds = sessions.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus().countsAsCompletedCook())
                    .map(CookingSession::getRecipeId)
                    .collect(Collectors.toSet());

            return (int) recipeRepository.findAllById(recipeIds).stream()
                    .filter(r -> cuisineType.equalsIgnoreCase(r.getCuisineType()))
                    .count();
        }

        List<Recipe> userRecipes = recipeRepository.findAllById(
                sessions.stream().map(CookingSession::getRecipeId).collect(Collectors.toSet()));
        return (int) userRecipes.stream()
                .filter(r -> cuisineType.equalsIgnoreCase(r.getCuisineType()))
                .count();
    }

    private int countSessionsByTechnique(String userId, String technique) {
        List<CookingSession> sessions = cookingSessionRepository
                .findTop20ByUserIdOrderByStartedAtDesc(userId);
        Set<String> recipeIds = sessions.stream()
            .filter(s -> s.getStatus() != null && s.getStatus().countsAsCompletedCook())
                .map(CookingSession::getRecipeId)
                .collect(Collectors.toSet());

        return (int) recipeRepository.findAllById(recipeIds).stream()
                .filter(r -> r.getSkillTags() != null && r.getSkillTags().stream()
                        .anyMatch(t -> technique.equalsIgnoreCase(t)))
                .count();
    }

    private int getStreakDays(String userId) {
        try {
            var stats = profileProvider.getAchievementStats(userId);
            return stats.getStreakCount();
        } catch (Exception e) {
            log.warn("Could not get streak for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    private int countMidnightCooks(String userId) {
        List<CookingSession> sessions = cookingSessionRepository
                .findTop20ByUserIdOrderByStartedAtDesc(userId);
        return (int) sessions.stream()
                .filter(s -> s.getStartedAt() != null)
                .filter(s -> {
                    int hour = s.getStartedAt().atOffset(ZoneOffset.UTC).getHour();
                    return hour >= 0 && hour < 5;
                })
                .count();
    }

    private int countBeatEstimatedTime(String userId) {
        List<CookingSession> sessions = cookingSessionRepository
                .findTop20ByUserIdOrderByStartedAtDesc(userId);
        Set<String> recipeIds = sessions.stream()
                .map(CookingSession::getRecipeId)
                .collect(Collectors.toSet());
        Map<String, Recipe> recipeMap = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r, (a, b) -> a));

        return (int) sessions.stream()
                .filter(s -> s.getCompletedAt() != null && s.getStartedAt() != null)
                .filter(s -> {
                    Recipe r = recipeMap.get(s.getRecipeId());
                    if (r == null || r.getTotalTimeMinutes() <= 0) return false;
                    long actual = java.time.Duration.between(s.getStartedAt(), s.getCompletedAt()).toMinutes();
                    return actual < r.getTotalTimeMinutes();
                })
                .count();
    }

    private int getFollowerCount(String userId) {
        try {
            var stats = profileProvider.getAchievementStats(userId);
            return (int) stats.getFollowerCount();
        } catch (Exception e) {
            log.warn("Could not get follower count for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    private int getTotalLikesReceived(String userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        return (int) recipes.stream().mapToLong(Recipe::getLikeCount).sum();
    }

    private int countOthersCookedYourRecipes(String userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        return (int) recipes.stream().mapToLong(Recipe::getCookCount).sum();
    }

    private void evaluateStreakAchievements(String userId, List<String> newlyUnlocked) {
        newlyUnlocked.addAll(evaluateCriteria(userId, CriteriaType.STREAK_DAYS, null));
    }


    private String derivePathName(String pathId) {
        if (pathId == null || pathId.equals("general")) return "General";
        String[] parts = pathId.split("_", 2);
        if (parts.length < 2) return capitalize(pathId);
        return capitalize(parts[1]);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
