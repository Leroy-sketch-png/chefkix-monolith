package com.chefkix.culinary.features.session.service;

import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.culinary.features.session.dto.response.CompletionResponse;
import com.chefkix.culinary.features.recipe.dto.request.RecipeCompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.culinary.features.session.dto.internal.SessionResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.RecipeCompletion;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.features.recipe.repository.CompletionRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.identity.api.ProfileProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamificationService {

    RecipeRepository recipeRepository;
    CompletionRepository completionRepository;
    ProfileProvider profileProvider;

    @Transactional
    public CompletionResponse completeRecipe(String recipeId, RecipeCompletionRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
                LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        long completedToday = completionRepository.countByUserIdAndCompletedAtAfter(
                userId, today.atStartOfDay());

        java.time.Instant accountCreatedAt = profileProvider.getAccountCreatedAt(userId);
        long accountAgeDays = accountCreatedAt != null
                ? java.time.temporal.ChronoUnit.DAYS.between(accountCreatedAt, java.time.Instant.now())
                : Long.MAX_VALUE;
        int dailyLimit = accountAgeDays < 7 ? 2 : 5;
        if (completedToday >= dailyLimit) {
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "You've reached the daily cooking limit (" + dailyLimit + " recipes).");
        }

        long totalElapsedSeconds = request.getTimerLogs() != null
                ? request.getTimerLogs().stream()
                    .mapToLong(RecipeCompletionRequest.TimerLog::getElapsedSeconds)
                    .sum()
                : 0;
        validateCookingTime(recipe.getTotalTimeMinutes(), totalElapsedSeconds);

        boolean hasProof = request.getProofImageUrls() != null && !request.getProofImageUrls().isEmpty();

        int finalXp;
        boolean isPublic;
        List<String> badgesToAward = new ArrayList<>();

        if (hasProof) {
            finalXp = recipe.getXpReward();
            isPublic = true;

            if (recipe.getRewardBadges() != null && !recipe.getRewardBadges().isEmpty()) {
                badgesToAward.addAll(recipe.getRewardBadges());
            }
            log.info("[COMPLETION] User {} completed PUBLIC. Full XP.", userId);
        } else {
            finalXp = (int) Math.ceil(recipe.getXpReward() * 0.5);
            isPublic = false;
            log.info("[COMPLETION] User {} completed PRIVATE. Half XP.", userId);
        }

        RecipeCompletion completion = RecipeCompletion.builder()
                .userId(userId)
                .recipeId(recipeId)
.proofImageUrls(request.getProofImageUrls())
                .actualDurationSeconds((int) totalElapsedSeconds)
.xpAwarded(finalXp)
                .isPublic(isPublic)
                .completedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        completionRepository.save(completion);

        CompletionRequest completionRequest = CompletionRequest.builder()
                .userId(userId)
                .xpAmount(finalXp)
                .sessionId(completion.getId())
                .recipeId(recipeId)
                .challengeCompleted(false)
                .newBadges(badgesToAward)
                .build();

        CompletionResult profileResult;
        try {
            profileResult = profileProvider.updateAfterCompletion(completionRequest);

            if (profileResult == null) {
                log.error("Profile Service returned null data for user {}", userId);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "No data received from Profile Service");
            }
        } catch (Exception e) {
            log.error("Error connecting to Profile Service: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error syncing user profile");
        }

        return CompletionResponse.builder()
                .completionId(completion.getId())
                .recipeId(recipeId)
                .xpEarned(finalXp)
                .newBadges(badgesToAward)
                .userProfile(CompletionResult.builder()
                        .userId(userId)
                        .currentXP(profileResult.getCurrentXP())
                        .currentXPGoal(profileResult.getCurrentXPGoal())
                        .currentLevel(profileResult.getCurrentLevel())
                        .completionCount(profileResult.getCompletionCount())
                        .build())
                .build();
    }

    public SessionResponse getSessionById(String sessionId) {
        RecipeCompletion session = completionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLETION_NOT_FOUND));

        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .completedAt(session.getCompletedAt())

                .pendingXp((double) session.getXpAwarded())

                .recipeId(recipe.getId())
                .recipeTitle(recipe.getTitle())

                .recipeAuthorId(recipe.getUserId())
                .recipeBaseXp((double) recipe.getXpReward())
                .build();
    }


    private void validateCookingTime(int recipeMinutes, long actualSeconds) {
        double actualMinutes = actualSeconds / 60.0;
        double minTime = recipeMinutes * 0.5;

        if (actualMinutes < minTime) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    String.format("Cooking time too fast (%d minutes). Minimum required: %.0f minutes.",
                            (int)actualMinutes, minTime));
        }
    }
}