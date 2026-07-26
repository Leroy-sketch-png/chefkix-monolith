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
import com.chefkix.culinary.features.challenge.entity.CommunityChallenge;
import com.chefkix.culinary.features.challenge.entity.SeasonalChallenge;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.challenge.repository.CommunityChallengeRedisRepository;
import com.chefkix.culinary.features.challenge.repository.CommunityChallengeRepository;
import com.chefkix.culinary.features.challenge.repository.SeasonalChallengeRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import java.time.Instant;
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
}
