package com.chefkix.culinary.provider;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.api.dto.CreatorInsightsInfo;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.interaction.entity.RecipeLike;
import com.chefkix.culinary.features.interaction.entity.RecipeSave;
import com.chefkix.culinary.features.interaction.repository.RecipeLikeRepository;
import com.chefkix.culinary.features.interaction.repository.RecipeSaveRepository;
import com.chefkix.culinary.features.recipe.events.RecipeIndexEvent;
import com.chefkix.culinary.features.achievement.repository.UserAchievementRepository;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.mealplan.repository.MealPlanRepository;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.recipe.service.RecipeService;
import com.chefkix.culinary.features.room.service.CookingRoomService;
import com.chefkix.culinary.features.report.dto.internal.InternalCreatorInsightsResponse;
import com.chefkix.culinary.features.shoppinglist.repository.CheckoutRecordRepository;
import com.chefkix.culinary.features.shoppinglist.repository.ShoppingListRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.ActiveCookingRedisRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provider implementation exposing culinary recipe data to other modules.
 * Delegates to internal RecipeService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeProviderImpl implements RecipeProvider {

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
        private final PantryItemRepository pantryItemRepository;
        private final MealPlanRepository mealPlanRepository;
        private final ShoppingListRepository shoppingListRepository;
        private final CheckoutRecordRepository checkoutRecordRepository;
        private final ChallengeLogRepository challengeLogRepository;
        private final UserAchievementRepository userAchievementRepository;
        private final RecipeLikeRepository recipeLikeRepository;
        private final RecipeSaveRepository recipeSaveRepository;
        private final CookingSessionRepository cookingSessionRepository;
        private final ActiveCookingRedisRepository activeCookingRepository;
        private final CookingRoomService cookingRoomService;
        private final ApplicationEventPublisher eventPublisher;

    @Override
    public CreatorInsightsInfo getCreatorInsights(String userId) {
        InternalCreatorInsightsResponse internal = recipeService.getRecipeWithAboveTenCooks(userId);
        int publishedCount = (int) recipeRepository.countByUserIdAndStatus(userId, RecipeStatus.PUBLISHED);

        List<CreatorInsightsInfo.TopRecipeInfo> highPerforming = internal.getHighPerformingRecipes() != null
                ? internal.getHighPerformingRecipes().stream().map(this::mapTopRecipe).collect(Collectors.toList())
                : List.of();

        return CreatorInsightsInfo.builder()
                .totalRecipesPublished(publishedCount)
                .avgRating(internal.getAvgRating() != null ? internal.getAvgRating() : 0.0)
                .topRecipes(internal.getTopRecipe() != null ? List.of(mapTopRecipe(internal.getTopRecipe())) : List.of())
                .highPerformingRecipes(highPerforming)
                .build();
    }

    private CreatorInsightsInfo.TopRecipeInfo mapTopRecipe(InternalCreatorInsightsResponse.TopRecipeDto dto) {
        return CreatorInsightsInfo.TopRecipeInfo.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .cookCount(dto.getCookCount())
                .xpGenerated(dto.getXpGenerated())
                .coverImageUrl(dto.getCoverImageUrl())
                .cookTimeMinutes(dto.getCookTimeMinutes())
                .difficulty(dto.getDifficulty())
                .averageRating(dto.getAverageRating())
                .build();
    }

    @Override
    public RecipeSummaryInfo getRecipeSummary(String recipeId) {
        return recipeRepository.findById(recipeId)
                .map(recipe -> RecipeSummaryInfo.builder()
                        .id(recipe.getId())
                        .title(recipe.getTitle())
                        .coverImageUrl(recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty()
                                ? recipe.getCoverImageUrl().get(0)
                                : null)
                        .authorId(recipe.getUserId())
                        .build())
                .orElse(null);
    }

        @Override
        public long cleanupDeletedUserData(String userId) {
                long affectedRecords = 0;

                List<com.chefkix.culinary.features.recipe.entity.Recipe> recipes = recipeRepository.findByUserId(userId);
                if (!recipes.isEmpty()) {
                        for (com.chefkix.culinary.features.recipe.entity.Recipe recipe : recipes) {
                                if (recipe.getStatus() != RecipeStatus.ARCHIVED) {
                                        recipe.setStatus(RecipeStatus.ARCHIVED);
                                        eventPublisher.publishEvent(RecipeIndexEvent.remove(recipe.getId()));
                                }
                        }
                        recipeRepository.saveAll(recipes);
                        affectedRecords += recipes.size();
                }

                long pantryItemsDeleted = pantryItemRepository.countByUserId(userId);
                if (pantryItemsDeleted > 0) {
                        pantryItemRepository.deleteAllByUserId(userId);
                        affectedRecords += pantryItemsDeleted;
                }

                List<?> mealPlans = mealPlanRepository.findByUserIdOrderByWeekStartDateDesc(userId);
                if (!mealPlans.isEmpty()) {
                        mealPlanRepository.deleteAllByUserId(userId);
                        affectedRecords += mealPlans.size();
                }

                long shoppingListsDeleted = shoppingListRepository.countByUserId(userId);
                if (shoppingListsDeleted > 0) {
                        shoppingListRepository.deleteAllByUserId(userId);
                        affectedRecords += shoppingListsDeleted;
                }

                List<?> checkoutRecords = checkoutRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
                if (!checkoutRecords.isEmpty()) {
                        checkoutRecordRepository.deleteAllByUserId(userId);
                        affectedRecords += checkoutRecords.size();
                }

                List<String> challengeDates = challengeLogRepository.findCompletedDatesByUserId(userId);
                if (!challengeDates.isEmpty()) {
                        challengeLogRepository.deleteAllByUserId(userId);
                        affectedRecords += challengeDates.size();
                }

                List<?> achievements = userAchievementRepository.findByUserId(userId);
                if (!achievements.isEmpty()) {
                        userAchievementRepository.deleteAllByUserId(userId);
                        affectedRecords += achievements.size();
                }

                List<RecipeLike> recipeLikes = recipeLikeRepository.findAllByUserId(userId);
                if (!recipeLikes.isEmpty()) {
                        recipeLikes.forEach(like -> recipeRepository.updateLikeCount(like.getRecipeId(), -1));
                        recipeLikeRepository.deleteAll(recipeLikes);
                        affectedRecords += recipeLikes.size();
                }

                List<RecipeSave> recipeSaves = recipeSaveRepository.findAllByUserId(userId);
                if (!recipeSaves.isEmpty()) {
                        recipeSaves.forEach(save -> recipeRepository.updateSaveCount(save.getRecipeId(), -1));
                        recipeSaveRepository.deleteAll(recipeSaves);
                        affectedRecords += recipeSaves.size();
                }

                List<CookingSession> sessions = cookingSessionRepository.findAllByUserId(userId);
                if (sessions != null && !sessions.isEmpty()) {
                        leaveDeletedUserRooms(sessions, userId);

                        LocalDateTime deletedAt = utcNow();
                        sessions.forEach(session -> anonymizeDeletedUserSession(session, deletedAt));
                        cookingSessionRepository.saveAll(sessions);
                        affectedRecords += sessions.size();
                }

                try {
                        activeCookingRepository.removeActive(userId);
                } catch (Exception e) {
                        log.warn("Failed to clear active cooking presence for deleted user {}: {}", userId, e.getMessage());
                }

                return affectedRecords;
        }

        private void leaveDeletedUserRooms(List<CookingSession> sessions, String userId) {
                sessions.stream()
                                .map(CookingSession::getRoomCode)
                                .filter(roomCode -> roomCode != null && !roomCode.isBlank())
                                .map(String::toUpperCase)
                                .distinct()
                                .forEach(roomCode -> {
                                        try {
                                                cookingRoomService.leaveRoom(userId, roomCode);
                                        } catch (Exception e) {
                                                log.warn("Failed to evict deleted user {} from cooking room {}: {}", userId, roomCode, e.getMessage());
                                        }
                                });
        }

        private void anonymizeDeletedUserSession(CookingSession session, LocalDateTime deletedAt) {
                session.setUserDeleted(true);
                session.setUserDeletedAt(deletedAt);
                session.setNotes(null);
                session.setRoomCode(null);
                session.setResumeDeadline(null);
                session.setPostDeadline(null);
                session.setPendingXp(0.0);
                session.setActiveTimers(new ArrayList<>());

                if (session.getStatus() == SessionStatus.IN_PROGRESS || session.getStatus() == SessionStatus.PAUSED) {
                        session.setStatus(SessionStatus.ABANDONED);
                        if (session.getAbandonedAt() == null) {
                                session.setAbandonedAt(deletedAt);
                        }
                }

                if (session.getStatus() == SessionStatus.POSTED) {
                        session.setStatus(SessionStatus.POST_DELETED);
                        session.setPostId(null);
                        if (session.getPostDeletedAt() == null) {
                                session.setPostDeletedAt(deletedAt);
                        }
                }
        }

        private LocalDateTime utcNow() {
                return LocalDateTime.now(ZoneOffset.UTC);
        }
}
