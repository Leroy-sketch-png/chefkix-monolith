package com.chefkix.culinary.features.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.common.helper.StreakCalculatorHelper;
import com.chefkix.culinary.features.challenge.dto.response.SeasonalChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.WeeklyChallengeResponse;
import com.chefkix.culinary.features.challenge.entity.CommunityChallenge;
import com.chefkix.culinary.features.challenge.entity.SeasonalChallenge;
import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import com.chefkix.culinary.features.challenge.model.ChallengeWeekKey;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.challenge.repository.CommunityChallengeRedisRepository;
import com.chefkix.culinary.features.challenge.repository.CommunityChallengeRepository;
import com.chefkix.culinary.features.challenge.repository.SeasonalChallengeRepository;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceLifecycleTest {

    @Mock private ChallengePoolService challengePoolService;
    @Mock private ChallengeLogRepository challengeLogRepository;
    @Mock private CommunityChallengeRepository communityChallengeRepository;
    @Mock private CommunityChallengeRedisRepository communityChallengeRedisRepository;
    @Mock private SeasonalChallengeRepository seasonalChallengeRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private CookingSessionRepository cookingSessionRepository;
    @Mock private StreakCalculatorHelper streakCalculator;
    @Mock private RecipeHelper recipeHelper;

    @InjectMocks private ChallengeService challengeService;

    @Test
    void seasonalReadExcludesEndedAndDerivesEffectiveStatusFromTime() {
        Instant now = Instant.now();
        SeasonalChallenge ended = seasonal("ended", now.minusSeconds(7200), now.minusSeconds(3600), "ACTIVE");
        SeasonalChallenge upcoming = seasonal("upcoming", now.plusSeconds(3600), now.plusSeconds(7200), "ACTIVE");
        SeasonalChallenge active = seasonal("active", now.minusSeconds(3600), now.plusSeconds(3600), "UPCOMING");

        when(seasonalChallengeRepository.findByStatusIn(List.of("ACTIVE", "UPCOMING")))
                .thenReturn(List.of(ended, upcoming, active));
        when(recipeRepository.findAllById(any())).thenReturn(List.of());
        when(challengeLogRepository.findByUserIdAndChallengeDate("user-1", "SEASONAL-active"))
                .thenReturn(Optional.empty());

        List<SeasonalChallengeResponse> result = challengeService.getSeasonalChallenges("user-1");

        assertThat(result).extracting(SeasonalChallengeResponse::getId)
                .containsExactly("upcoming", "active");
        assertThat(result).extracting(SeasonalChallengeResponse::getStatus)
                .containsExactly("UPCOMING", "ACTIVE");
    }

    @Test
    void futureCommunityChallengeIsNeitherVisibleNorAdvanced() {
        Instant now = Instant.now();
        CommunityChallenge future = CommunityChallenge.builder()
                .id("future-community")
                .status("ACTIVE")
                .startsAt(now.plusSeconds(3600))
                .endsAt(now.plusSeconds(7200))
                .criteria(Map.of("type", "COOK_ANY"))
                .build();
        when(communityChallengeRepository.findByStatusAndEndsAtAfter(eq("ACTIVE"), any()))
                .thenReturn(List.of(future));

        assertThat(challengeService.getActiveCommunityChallenge("user-1")).isEmpty();
        challengeService.checkAndAdvanceCommunityChallenge("user-1", Recipe.builder().id("recipe-1").build());

        verifyNoInteractions(communityChallengeRedisRepository);
        verify(communityChallengeRepository, never()).save(any());
    }

    @Test
    void futureSeasonalChallengeCannotAwardProgress() {
        Instant now = Instant.now();
        SeasonalChallenge future = seasonal("future", now.plusSeconds(3600), now.plusSeconds(7200), "UPCOMING");
        when(seasonalChallengeRepository.findByStatusIn(List.of("ACTIVE", "UPCOMING")))
                .thenReturn(List.of(future));

        assertThat(challengeService.checkAndAdvanceSeasonalChallenge(
                "user-1", Recipe.builder().id("recipe-1").build())).isEmpty();

        verify(challengeLogRepository, never()).save(any());
    }

    @Test
    void weeklyProgressCountsQualifyingRecipesBeyondTheFiveItemPreview() {
        ChallengeDefinition weekly = weeklyChallenge(6);
        List<Recipe> recipes = List.of(
                italianRecipe("recipe-1"), italianRecipe("recipe-2"), italianRecipe("recipe-3"),
                italianRecipe("recipe-4"), italianRecipe("recipe-5"), italianRecipe("recipe-6"));
        List<CookingSession> sessions = recipes.stream()
                .map(recipe -> completedSession(recipe.getId()))
                .toList();

        when(challengePoolService.getThisWeekChallenge()).thenReturn(weekly);
        when(cookingSessionRepository.findByUserIdAndStatusAndCompletedAtBetween(
                eq("user-1"), eq(SessionStatus.COMPLETED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(sessions);
        when(recipeRepository.findAllById(any())).thenReturn(recipes);
        when(recipeRepository.findTop5ByCuisineTypeInIgnoreCase(List.of("Italian")))
                .thenReturn(recipes.subList(0, 5));
        when(challengeLogRepository.existsByUserIdAndChallengeDate(eq("user-1"), any()))
                .thenReturn(false);

        WeeklyChallengeResponse response = challengeService.getWeeklyChallenge("user-1");

        assertThat(response.getProgress()).isEqualTo(6);
        assertThat(response.getMatchingRecipes()).hasSize(5);
    }

    @Test
    void weeklyAwardAddsTheTriggeringPrePersistenceCookExactlyOnce() {
        ChallengeDefinition weekly = weeklyChallenge(3);
        Recipe first = italianRecipe("recipe-1");
        Recipe second = italianRecipe("recipe-2");
        Recipe triggering = italianRecipe("recipe-3");

        when(challengePoolService.getThisWeekChallenge()).thenReturn(weekly);
        when(challengeLogRepository.existsByUserIdAndChallengeDate(eq("user-1"), any()))
                .thenReturn(false);
        when(cookingSessionRepository.findByUserIdAndStatusAndCompletedAtBetween(
                eq("user-1"), eq(SessionStatus.COMPLETED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession(first.getId()), completedSession(second.getId())));
        when(recipeRepository.findAllById(any())).thenReturn(List.of(first, second));

        var result = challengeService.checkAndCompleteWeeklyChallenge("user-1", triggering);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getChallengeKind()).isEqualTo("WEEKLY");
        verify(challengeLogRepository).save(any());
    }

    @Test
    void weeklyProgressExcludesMissingAndNonmatchingRecipes() {
        ChallengeDefinition weekly = weeklyChallenge(10);
        Recipe qualifying = italianRecipe("recipe-1");
        Recipe nonmatching = Recipe.builder().id("recipe-2").cuisineType("French").build();

        when(challengePoolService.getThisWeekChallenge()).thenReturn(weekly);
        when(cookingSessionRepository.findByUserIdAndStatusAndCompletedAtBetween(
                eq("user-1"), eq(SessionStatus.COMPLETED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        completedSession(qualifying.getId()),
                        completedSession(nonmatching.getId()),
                        completedSession("deleted-recipe")));
        when(recipeRepository.findAllById(any())).thenReturn(List.of(qualifying, nonmatching));
        when(recipeRepository.findTop5ByCuisineTypeInIgnoreCase(List.of("Italian")))
                .thenReturn(List.of(qualifying));
        when(challengeLogRepository.existsByUserIdAndChallengeDate(eq("user-1"), any()))
                .thenReturn(false);

        assertThat(challengeService.getWeeklyChallenge("user-1").getProgress()).isEqualTo(1);
    }

    @Test
    void weeklyIdentityUsesTheIsoWeekBasedYearAcrossNewYear() {
        assertThat(ChallengeWeekKey.from(LocalDate.of(2025, 12, 29)))
                .isEqualTo("WEEKLY-2026-W01");
        assertThat(ChallengeWeekKey.from(LocalDate.of(2026, 1, 1)))
                .isEqualTo("WEEKLY-2026-W01");
    }

    private SeasonalChallenge seasonal(String id, Instant startsAt, Instant endsAt, String status) {
        return SeasonalChallenge.builder()
                .id(id)
                .title(id)
                .targetCount(1)
                .targetUnit("recipes")
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(status)
                .featuredRecipeIds(List.of("recipe-1"))
                .criteria(Map.of("type", "COOK_ANY"))
                .build();
    }

    private ChallengeDefinition weeklyChallenge(int target) {
        return ChallengeDefinition.builder()
                .id("weekly-italian")
                .title("Italian Week")
                .target(target)
                .bonusXp(150)
                .criteriaMetadata(Map.of("cuisineType", List.of("Italian")))
                .validationLogic(recipe -> "Italian".equalsIgnoreCase(recipe.getCuisineType()))
                .build();
    }

    private Recipe italianRecipe(String id) {
        return Recipe.builder().id(id).title(id).cuisineType("Italian").build();
    }

    private CookingSession completedSession(String recipeId) {
        return CookingSession.builder()
                .userId("user-1")
                .recipeId(recipeId)
                .status(SessionStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
    }
}
