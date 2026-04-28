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
        // 1. GET USER ID FROM TOKEN (Security - server determines identity)
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
                LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 2. FIND ORIGINAL RECIPE
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 3. RATE LIMIT CHECK (Anti-spam: 5 times/day limit)
        long completedToday = completionRepository.countByUserIdAndCompletedAtAfter(
                userId, today.atStartOfDay());

        // Account age-based rate limiting: new accounts (< 7 days) get stricter limits
        java.time.Instant accountCreatedAt = profileProvider.getAccountCreatedAt(userId);
        long accountAgeDays = accountCreatedAt != null
                ? java.time.temporal.ChronoUnit.DAYS.between(accountCreatedAt, java.time.Instant.now())
                : Long.MAX_VALUE;
        int dailyLimit = accountAgeDays < 7 ? 2 : 5;
        if (completedToday >= dailyLimit) {
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "You've reached the daily cooking limit (" + dailyLimit + " recipes).");
        }

        // 4. TIME VALIDATION (Anti-Cheat)
        long totalElapsedSeconds = request.getTimerLogs() != null
                ? request.getTimerLogs().stream()
                    .mapToLong(RecipeCompletionRequest.TimerLog::getElapsedSeconds)
                    .sum()
                : 0;
        validateCookingTime(recipe.getTotalTimeMinutes(), totalElapsedSeconds);

        // 5. SCORING LOGIC (HYBRID XP & BADGE)
        // Check if user submitted proof photos
        boolean hasProof = request.getProofImageUrls() != null && !request.getProofImageUrls().isEmpty();

        int finalXp;
        boolean isPublic;
        List<String> badgesToAward = new ArrayList<>();

        if (hasProof) {
            // CASE A: HAS PHOTOS -> Full XP + Eligible for Badges
            finalXp = recipe.getXpReward();
            isPublic = true;

            // Only award badges if recipe has badge configuration
            if (recipe.getRewardBadges() != null && !recipe.getRewardBadges().isEmpty()) {
                badgesToAward.addAll(recipe.getRewardBadges());
            }
            log.info("[COMPLETION] User {} completed PUBLIC. Full XP.", userId);
        } else {
            // CASE B: NO PHOTOS -> Half XP + No Badges
            finalXp = (int) Math.ceil(recipe.getXpReward() * 0.5);
            isPublic = false;
            log.info("[COMPLETION] User {} completed PRIVATE. Half XP.", userId);
        }

        // 6. SAVE HISTORY (To Recipe Service MongoDB)
        RecipeCompletion completion = RecipeCompletion.builder()
                .userId(userId)
                .recipeId(recipeId)
                .proofImageUrls(request.getProofImageUrls()) // Save photos from request
                .actualDurationSeconds((int) totalElapsedSeconds)
                .xpAwarded(finalXp)      // Save actual XP awarded
                .isPublic(isPublic)
                .completedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        completionRepository.save(completion);

        // 7. CALL PROFILE SERVICE (Sync)
        // Create Internal DTO to send (filling in server-calculated values)
        CompletionRequest completionRequest = CompletionRequest.builder()
                .userId(userId)
                .xpAmount(finalXp)
                .recipeId(recipeId)
                .challengeCompleted(false)
                .newBadges(badgesToAward)
                .build();

        CompletionResult profileResult;
        try {
            // Direct method call — no Feign wrapping
            profileResult = profileProvider.updateAfterCompletion(completionRequest);

            if (profileResult == null) {
                log.error("Profile Service returned null data for user {}", userId);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "No data received from Profile Service");
            }
        } catch (Exception e) {
            log.error("Error connecting to Profile Service: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error syncing user profile");
        }

        // 8. MAP DATA AND RETURN (For Frontend)
        // Combine this completion's data with latest user data from Profile Service
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
        // 1. Find cooking session (RecipeCompletion)
        RecipeCompletion session = completionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLETION_NOT_FOUND));

        // 2. Find original recipe (to get author info and title)
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 3. Map to DTO for Post Service
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .completedAt(session.getCompletedAt())

                // Get XP user received when cooking was completed (Pending XP)
                .pendingXp((double) session.getXpAwarded())

                // Recipe info
                .recipeId(recipe.getId())
                .recipeTitle(recipe.getTitle())

                // Important: Get author ID so Post Service can calculate 4% bonus
                .recipeAuthorId(recipe.getUserId())
                .recipeBaseXp((double) recipe.getXpReward())
                .build();
    }

    // --- Helper Methods ---

    private void validateCookingTime(int recipeMinutes, long actualSeconds) {
        double actualMinutes = actualSeconds / 60.0;
        // Allow up to 50% faster (e.g. 60min recipe done in 20min is cheating)
        double minTime = recipeMinutes * 0.5;

        if (actualMinutes < minTime) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    String.format("Cooking time too fast (%d minutes). Minimum required: %.0f minutes.",
                            (int)actualMinutes, minTime));
        }
    }
}