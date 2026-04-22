package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.identity.dto.request.internal.InternalCompletionRequest;
import com.chefkix.identity.dto.response.RecipeCompletionResponse;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.shared.service.KafkaIdempotencyService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private SettingsService settingsService;
    @Mock
    private BlockService blockService;
    @Mock
    private KafkaIdempotencyService idempotencyService;
    @Mock
    @Lazy
    private RecipeProvider recipeProvider;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void updateAfterCompletionSkipsDuplicateIdempotencyKeyWithoutSavingAgain() {
        String userId = "user-1";
        String idempotencyKey = "xp:COOKING_SESSION:user-1:session-1";
        Statistics stats = Statistics.builder()
                .currentLevel(3)
                .currentXP(275.0)
                .currentXPGoal(500.0)
                .completionCount(7L)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .statistics(stats)
                .build();
        InternalCompletionRequest request = InternalCompletionRequest.builder()
                .userId(userId)
                .xpAmount(90)
                .idempotencyKey(idempotencyKey)
                .build();

        when(idempotencyService.tryProcess(idempotencyKey, "xp-delivery")).thenReturn(false);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        RecipeCompletionResponse response = statisticsService.updateAfterCompletion(request);

        assertThat(response.getCurrentLevel()).isEqualTo(3);
        assertThat(response.getCurrentXP()).isEqualTo(275);
        assertThat(response.getCompletionCount()).isEqualTo(7L);
        assertThat(response.getLeveledUp()).isFalse();
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(idempotencyService, never()).removeProcessed(any(), any());
    }

    @Test
    void updateAfterCompletionRemovesReservationWhenSaveFails() {
        String userId = "user-1";
        String idempotencyKey = "xp:COOKING_SESSION:user-1:session-1";
        Statistics stats = Statistics.builder()
                .currentLevel(1)
                .currentXP(0.0)
                .currentXPGoal(1000.0)
                .completionCount(0L)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .statistics(stats)
                .build();
        InternalCompletionRequest request = InternalCompletionRequest.builder()
                .userId(userId)
                .xpAmount(120)
                .idempotencyKey(idempotencyKey)
                .build();

        when(idempotencyService.tryProcess(idempotencyKey, "xp-delivery")).thenReturn(true);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenThrow(new RuntimeException("db write failed"));

        assertThrows(RuntimeException.class, () -> statisticsService.updateAfterCompletion(request));

        verify(idempotencyService).removeProcessed(idempotencyKey, "xp-delivery");
    }

        @Test
        void updateAfterCompletionAppliesCookingAndChallengeProgression() {
                String userId = "user-1";
                String idempotencyKey = "xp:COOKING_SESSION:user-1:session-1";
                Instant now = Instant.now();
                Statistics stats = Statistics.builder()
                                .currentLevel(1)
                                .currentXP(100.0)
                                .currentXPGoal(1000.0)
                                .completionCount(2L)
                                .streakCount(2)
                                .longestStreak(2)
                                .challengeStreak(1)
                                .lastCookAt(now.minus(Duration.ofHours(24)))
                                .lastChallengeAt(now.minus(Duration.ofHours(12)))
                                .build();
                UserProfile profile = UserProfile.builder()
                                .userId(userId)
                                .statistics(stats)
                                .build();
                InternalCompletionRequest request = InternalCompletionRequest.builder()
                                .userId(userId)
                                .xpAmount(120)
                                .recipeId("recipe-1")
                                .challengeCompleted(true)
                                .idempotencyKey(idempotencyKey)
                                .build();

                when(idempotencyService.tryProcess(idempotencyKey, "xp-delivery")).thenReturn(true);
                when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
                when(userProfileRepository.save(profile)).thenReturn(profile);

                RecipeCompletionResponse response = statisticsService.updateAfterCompletion(request);

                assertThat(stats.getCompletionCount()).isEqualTo(3L);
                assertThat(stats.getStreakCount()).isEqualTo(3);
                assertThat(stats.getLongestStreak()).isEqualTo(3);
                assertThat(stats.getChallengeStreak()).isEqualTo(2);
                assertThat(stats.getLastCookAt()).isNotNull();
                assertThat(stats.getLastChallengeAt()).isNotNull();
                assertThat(stats.getRecipeCookCounts()).containsEntry("recipe-1", 1);
                assertThat(stats.getRecipesCooked()).isEqualTo(1L);
                assertThat(stats.getRecipesMastered()).isZero();
                assertThat(response.getCompletionCount()).isEqualTo(3L);
                verify(userProfileRepository).save(profile);
        }
}