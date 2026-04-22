package com.chefkix.culinary.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.features.achievement.entity.UserAchievement;
import com.chefkix.culinary.features.achievement.repository.UserAchievementRepository;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.interaction.entity.RecipeLike;
import com.chefkix.culinary.features.interaction.entity.RecipeSave;
import com.chefkix.culinary.features.interaction.repository.RecipeLikeRepository;
import com.chefkix.culinary.features.interaction.repository.RecipeSaveRepository;
import com.chefkix.culinary.features.mealplan.entity.MealPlan;
import com.chefkix.culinary.features.mealplan.repository.MealPlanRepository;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.room.service.CookingRoomService;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.ActiveCookingRedisRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.features.shoppinglist.entity.CheckoutRecord;
import com.chefkix.culinary.features.shoppinglist.repository.CheckoutRecordRepository;
import com.chefkix.culinary.features.shoppinglist.repository.ShoppingListRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RecipeProviderImplTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private PantryItemRepository pantryItemRepository;
    @Mock
    private MealPlanRepository mealPlanRepository;
    @Mock
    private ShoppingListRepository shoppingListRepository;
    @Mock
    private CheckoutRecordRepository checkoutRecordRepository;
    @Mock
    private ChallengeLogRepository challengeLogRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
        @Mock
        private RecipeLikeRepository recipeLikeRepository;
        @Mock
        private RecipeSaveRepository recipeSaveRepository;
        @Mock
        private CookingSessionRepository cookingSessionRepository;
        @Mock
        private ActiveCookingRedisRepository activeCookingRedisRepository;
        @Mock
        private CookingRoomService cookingRoomService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RecipeProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new RecipeProviderImpl(
                                null,
                recipeRepository,
                pantryItemRepository,
                mealPlanRepository,
                shoppingListRepository,
                checkoutRecordRepository,
                challengeLogRepository,
                userAchievementRepository,
                                recipeLikeRepository,
                                recipeSaveRepository,
                                cookingSessionRepository,
                                activeCookingRedisRepository,
                                cookingRoomService,
                eventPublisher);
    }

    @Test
        void cleanupDeletedUserDataArchivesRecipesDeletesPersonalPlanningStateAndRemovesInteractions() {
        String userId = "user-1";

        Recipe publishedRecipe = Recipe.builder()
                .id("recipe-1")
                .userId(userId)
                .status(RecipeStatus.PUBLISHED)
                .build();
        Recipe archivedRecipe = Recipe.builder()
                .id("recipe-2")
                .userId(userId)
                .status(RecipeStatus.ARCHIVED)
                .build();

        when(recipeRepository.findByUserId(userId)).thenReturn(List.of(publishedRecipe, archivedRecipe));
        when(pantryItemRepository.countByUserId(userId)).thenReturn(1L);
        when(mealPlanRepository.findByUserIdOrderByWeekStartDateDesc(userId))
                .thenReturn(List.of(org.mockito.Mockito.mock(MealPlan.class), org.mockito.Mockito.mock(MealPlan.class)));
        when(shoppingListRepository.countByUserId(userId)).thenReturn(3L);
        when(checkoutRecordRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(org.mockito.Mockito.mock(CheckoutRecord.class)));
        when(challengeLogRepository.findCompletedDatesByUserId(userId)).thenReturn(List.of("2026-04-20", "2026-04-21"));
        when(userAchievementRepository.findByUserId(userId))
                .thenReturn(List.of(org.mockito.Mockito.mock(UserAchievement.class)));
        RecipeLike likeOne = RecipeLike.builder().id("like-1").recipeId("recipe-a").userId(userId).build();
        RecipeLike likeTwo = RecipeLike.builder().id("like-2").recipeId("recipe-b").userId(userId).build();
        RecipeSave saveOne = RecipeSave.builder().id("save-1").recipeId("recipe-b").userId(userId).build();
        when(recipeLikeRepository.findAllByUserId(userId)).thenReturn(List.of(likeOne, likeTwo));
        when(recipeSaveRepository.findAllByUserId(userId)).thenReturn(List.of(saveOne));
        when(cookingSessionRepository.findAllByUserId(userId)).thenReturn(List.of());

        long affectedRecords = provider.cleanupDeletedUserData(userId);

        assertThat(affectedRecords).isEqualTo(15);
        assertThat(publishedRecipe.getStatus()).isEqualTo(RecipeStatus.ARCHIVED);
        assertThat(archivedRecipe.getStatus()).isEqualTo(RecipeStatus.ARCHIVED);

        verify(recipeRepository).saveAll(List.of(publishedRecipe, archivedRecipe));
        verify(recipeRepository).updateLikeCount("recipe-a", -1);
        verify(recipeRepository).updateLikeCount("recipe-b", -1);
        verify(recipeRepository).updateSaveCount("recipe-b", -1);
        verify(pantryItemRepository).deleteAllByUserId(userId);
        verify(mealPlanRepository).deleteAllByUserId(userId);
        verify(shoppingListRepository).deleteAllByUserId(userId);
        verify(checkoutRecordRepository).deleteAllByUserId(userId);
        verify(challengeLogRepository).deleteAllByUserId(userId);
        verify(userAchievementRepository).deleteAllByUserId(userId);
        verify(recipeLikeRepository).deleteAll(List.of(likeOne, likeTwo));
        verify(recipeSaveRepository).deleteAll(List.of(saveOne));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
        verify(activeCookingRedisRepository).removeActive(userId);
    }

    @Test
    void cleanupDeletedUserDataScrubsSessionsPreservesCookSignalsAndClearsLiveState() {
        String userId = "user-1";
        LocalDateTime deadline = LocalDateTime.of(2026, 4, 23, 12, 0);

        CookingSession activeSession = CookingSession.builder()
                .id("session-active")
                .userId(userId)
                .status(com.chefkix.culinary.common.enums.SessionStatus.IN_PROGRESS)
                .roomCode("room1")
                .notes("private notes")
                .pendingXp(15.0)
                .postDeadline(deadline)
                .activeTimers(new ArrayList<>(List.of(CookingSession.ActiveTimer.builder().stepNumber(1).build())))
                .build();
        CookingSession completedSession = CookingSession.builder()
                .id("session-completed")
                .userId(userId)
                .status(com.chefkix.culinary.common.enums.SessionStatus.COMPLETED)
                .notes("keep aggregate, drop note")
                .pendingXp(70.0)
                .postDeadline(deadline)
                .build();
        CookingSession postedSession = CookingSession.builder()
                .id("session-posted")
                .userId(userId)
                .status(com.chefkix.culinary.common.enums.SessionStatus.POSTED)
                .roomCode("room1")
                .postId("post-1")
                .pendingXp(0.0)
                .build();

        when(recipeRepository.findByUserId(userId)).thenReturn(List.of());
        when(pantryItemRepository.countByUserId(userId)).thenReturn(0L);
        when(mealPlanRepository.findByUserIdOrderByWeekStartDateDesc(userId)).thenReturn(List.of());
        when(shoppingListRepository.countByUserId(userId)).thenReturn(0L);
        when(checkoutRecordRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(challengeLogRepository.findCompletedDatesByUserId(userId)).thenReturn(List.of());
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());
        when(recipeLikeRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(recipeSaveRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(cookingSessionRepository.findAllByUserId(userId))
                .thenReturn(List.of(activeSession, completedSession, postedSession));

        long affectedRecords = provider.cleanupDeletedUserData(userId);

        assertThat(affectedRecords).isEqualTo(3);

        assertThat(activeSession.isUserDeleted()).isTrue();
        assertThat(activeSession.getUserDeletedAt()).isNotNull();
        assertThat(activeSession.getStatus()).isEqualTo(com.chefkix.culinary.common.enums.SessionStatus.ABANDONED);
        assertThat(activeSession.getAbandonedAt()).isNotNull();
        assertThat(activeSession.getRoomCode()).isNull();
        assertThat(activeSession.getNotes()).isNull();
        assertThat(activeSession.getPendingXp()).isEqualTo(0.0);
        assertThat(activeSession.getPostDeadline()).isNull();
        assertThat(activeSession.getActiveTimers()).isEmpty();

        assertThat(completedSession.isUserDeleted()).isTrue();
        assertThat(completedSession.getStatus()).isEqualTo(com.chefkix.culinary.common.enums.SessionStatus.COMPLETED);
        assertThat(completedSession.getNotes()).isNull();
        assertThat(completedSession.getPendingXp()).isEqualTo(0.0);
        assertThat(completedSession.getPostDeadline()).isNull();

        assertThat(postedSession.isUserDeleted()).isTrue();
        assertThat(postedSession.getStatus()).isEqualTo(com.chefkix.culinary.common.enums.SessionStatus.POST_DELETED);
        assertThat(postedSession.getPostId()).isNull();
        assertThat(postedSession.getPostDeletedAt()).isNotNull();
        assertThat(postedSession.getRoomCode()).isNull();

        verify(cookingRoomService).leaveRoom(userId, "ROOM1");
        verify(cookingSessionRepository).saveAll(List.of(activeSession, completedSession, postedSession));
        verify(activeCookingRedisRepository).removeActive(userId);
    }
}