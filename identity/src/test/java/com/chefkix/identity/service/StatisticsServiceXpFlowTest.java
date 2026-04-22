package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.shared.event.GamificationNotificationEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceXpFlowTest {

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
    private RecipeProvider recipeProvider;

    @InjectMocks
    private StatisticsService statisticsService;

    private UserProfile profile;
    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = Statistics.builder()
                .currentLevel(1)
                .currentXP(100.0)
                .currentXPGoal(1000.0)
                .xpWeekly(100.0)
                .xpMonthly(100.0)
                .completionCount(7L)
                .streakCount(4)
                .longestStreak(4)
                .lastCookAt(Instant.now().minus(Duration.ofHours(6)))
                .challengeStreak(2)
                .lastChallengeAt(Instant.now().minus(Duration.ofHours(12)))
                .badges(List.of("existing-badge"))
                .badgeTimestamps(new HashMap<>())
                .recipeCookCounts(new HashMap<>())
                .recipesCooked(0L)
                .recipesMastered(0L)
                .build();

        profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Chef Kix")
                .statistics(stats)
                .build();

        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));
    }

    @Test
    void rewardXpFullDoesNotApplyCookingProgressForLinkingPostEvents() {
        Instant originalLastCookAt = stats.getLastCookAt();

        statisticsService.rewardXpFull(
                "user-1",
                70.0,
                List.of("broth-master"),
                false,
                "recipe-1",
                "LINKING_POST",
                "session-1");

        assertThat(stats.getCurrentXP()).isEqualTo(170.0);
        assertThat(stats.getCompletionCount()).isEqualTo(7L);
        assertThat(stats.getStreakCount()).isEqualTo(4);
        assertThat(stats.getLastCookAt()).isEqualTo(originalLastCookAt);
        assertThat(stats.getBadges()).containsExactlyInAnyOrder("existing-badge", "broth-master");

        ArgumentCaptor<GamificationNotificationEvent> eventCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
            verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.<String>eq("gamification-delivery"), eventCaptor.capture());
        GamificationNotificationEvent event = eventCaptor.getValue();
        assertThat(event.getSource()).isEqualTo("LINKING_POST");
        assertThat(event.getRecipeId()).isEqualTo("recipe-1");
        assertThat(event.getSessionId()).isEqualTo("session-1");
        assertThat(event.getNewBadges()).containsExactly("broth-master");
    }

    @Test
    void rewardXpFullSendsGamificationEventForXpOnlyLinkPostReward() {
        statisticsService.rewardXpFull(
                "user-1",
                55.0,
                List.of(),
                false,
                "recipe-1",
                "LINKING_POST",
                "session-1");

        ArgumentCaptor<GamificationNotificationEvent> eventCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
            verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.<String>eq("gamification-delivery"), eventCaptor.capture());
        GamificationNotificationEvent event = eventCaptor.getValue();
        assertThat(event.getXpEarned()).isEqualTo(55.0);
        assertThat(event.isLeveledUp()).isFalse();
        assertThat(event.getNewBadges()).isEmpty();
        assertThat(event.getSource()).isEqualTo("LINKING_POST");
        assertThat(event.getRecipeId()).isEqualTo("recipe-1");
        assertThat(event.getSessionId()).isEqualTo("session-1");
    }

    @Test
    void rewardXpFullAppliesCookingProgressOnlyForCookingSessionEvents() {
        statisticsService.rewardXpFull(
                "user-1",
                40.0,
                List.of(),
                true,
                "recipe-1",
                "COOKING_SESSION",
                "session-1");

        assertThat(stats.getCurrentXP()).isEqualTo(140.0);
        assertThat(stats.getCompletionCount()).isEqualTo(8L);
        assertThat(stats.getStreakCount()).isEqualTo(5);
        assertThat(stats.getChallengeStreak()).isEqualTo(3);
        assertThat(stats.getRecipeCookCounts()).containsEntry("recipe-1", 1);
        assertThat(stats.getRecipesCooked()).isEqualTo(1L);
    }
}