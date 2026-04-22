package com.chefkix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.identity.api.NotificationPreferencesProvider;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.service.BlockService;
import com.chefkix.identity.service.SettingsService;
import com.chefkix.identity.service.StatisticsService;
import com.chefkix.notification.dto.response.NotificationResponse;
import com.chefkix.notification.entity.Notification;
import com.chefkix.notification.enums.NotificationType;
import com.chefkix.notification.listener.BellNotificationListener;
import com.chefkix.notification.mapper.NotificationMapper;
import com.chefkix.notification.repository.NotificationRepository;
import com.chefkix.notification.service.NotificationService;
import com.chefkix.notification.service.PushNotificationService;
import com.chefkix.shared.event.GamificationNotificationEvent;
import com.chefkix.shared.event.ReminderEvent;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class XpNotificationContractTest {

    @Mock
    private MongoTemplate identityMongoTemplate;
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
    private KafkaIdempotencyService statisticsIdempotencyService;
    @Mock
    private RecipeProvider recipeProvider;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private NotificationPreferencesProvider notificationPreferencesProvider;
    @Mock
    private MongoTemplate notificationMongoTemplate;
    @Mock
    private KafkaIdempotencyService listenerIdempotencyService;

    private StatisticsService statisticsService;
    private BellNotificationListener bellNotificationListener;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(
                identityMongoTemplate,
                userProfileRepository,
                profileMapper,
                kafkaTemplate,
                settingsService,
                blockService,
                statisticsIdempotencyService,
                recipeProvider);

        NotificationService notificationService = new NotificationService(
                notificationRepository,
                notificationMapper,
                messagingTemplate,
                pushNotificationService,
                notificationPreferencesProvider,
                notificationMongoTemplate);
        bellNotificationListener = new BellNotificationListener(notificationService, listenerIdempotencyService);

        when(notificationMapper.toNotificationResponse(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            return NotificationResponse.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .isRead(notification.getIsRead())
                    .content(notification.getContent())
                    .targetEntityId(notification.getTargetEntityId())
                    .targetEntityUrl(notification.getTargetEntityUrl())
                    .createdAt(notification.getCreatedAt())
                    .count(notification.getCount())
                    .latestActorId(notification.getLatestActorId())
                    .latestActorName(notification.getLatestActorName())
                    .latestActorAvatarUrl(notification.getLatestActorAvatarUrl())
                    .build();
        });
    }

    @Test
    void linkingPostXpProducedByStatisticsServiceBecomesXpAwardedBellNotification() {
        Statistics stats = Statistics.builder()
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
        UserProfile profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Chef Kix")
                .statistics(stats)
                .build();

        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));
        when(listenerIdempotencyService.tryProcess(anyString(), eq("gamification-delivery"))).thenReturn(true);
        when(notificationPreferencesProvider.isNotificationEnabled("user-1", "xpAndLevelUps")).thenReturn(true);

        statisticsService.rewardXpFull(
                "user-1",
                55.0,
                List.of(),
                false,
                "recipe-1",
                "LINKING_POST",
                "session-1");

        ArgumentCaptor<GamificationNotificationEvent> eventCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
        verify(kafkaTemplate).send(eq("gamification-delivery"), eventCaptor.capture());
        GamificationNotificationEvent event = eventCaptor.getValue();

        bellNotificationListener.listenGamificationDelivery(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();

        assertThat(event.getSource()).isEqualTo("LINKING_POST");
        assertThat(event.getRecipeId()).isEqualTo("recipe-1");
        assertThat(event.getSessionId()).isEqualTo("session-1");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.XP_AWARDED);
        assertThat(savedNotification.getTargetEntityId()).isEqualTo("session-1");
        assertThat(savedNotification.getContent()).contains("55 XP");
        verify(listenerIdempotencyService).tryProcess(event.getEventId(), "gamification-delivery");
    }

    @Test
    void cookingSessionLevelUpAndBadgeProducedByStatisticsServiceBecomeOrderedBellNotifications() {
        Statistics stats = Statistics.builder()
                .currentLevel(1)
                .currentXP(990.0)
                .currentXPGoal(1000.0)
                .xpWeekly(990.0)
                .xpMonthly(990.0)
                .completionCount(4L)
                .streakCount(2)
                .longestStreak(2)
                .lastCookAt(Instant.now().minus(Duration.ofHours(6)))
                .challengeStreak(1)
                .lastChallengeAt(Instant.now().minus(Duration.ofHours(12)))
                .badges(List.of("existing-badge"))
                .badgeTimestamps(new HashMap<>())
                .recipeCookCounts(new HashMap<>())
                .recipesCooked(0L)
                .recipesMastered(0L)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Chef Kix")
                .statistics(stats)
                .build();

        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));
        when(listenerIdempotencyService.tryProcess(anyString(), eq("gamification-delivery"))).thenReturn(true);
        when(notificationPreferencesProvider.isNotificationEnabled("user-1", "xpAndLevelUps")).thenReturn(true);
        when(notificationPreferencesProvider.isNotificationEnabled("user-1", "badges")).thenReturn(true);

        statisticsService.rewardXpFull(
                "user-1",
                25.0,
                List.of("broth-master"),
                false,
                "recipe-1",
                "COOKING_SESSION",
                "session-1");

        ArgumentCaptor<GamificationNotificationEvent> eventCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
        verify(kafkaTemplate).send(eq("gamification-delivery"), eventCaptor.capture());
        GamificationNotificationEvent event = eventCaptor.getValue();

        bellNotificationListener.listenGamificationDelivery(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        assertThat(event.isLeveledUp()).isTrue();
        assertThat(event.getNewLevel()).isEqualTo(2);
        assertThat(event.getNewBadges()).containsExactly("broth-master");
        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getType)
                .containsExactly(NotificationType.LEVEL_UP, NotificationType.BADGE_EARNED);
        assertThat(notificationCaptor.getAllValues().get(0).getContent()).contains("Level 2");
        assertThat(notificationCaptor.getAllValues().get(1).getContent()).contains("broth-master");
        verify(listenerIdempotencyService).tryProcess(event.getEventId(), "gamification-delivery");
    }

    @Test
    void creatorBonusLevelUpProducesLevelUpAndCreatorBonusNotificationsWithoutXpAwardedRow() {
        Statistics stats = Statistics.builder()
                .currentLevel(1)
                .currentXP(995.0)
                .currentXPGoal(1000.0)
                .xpWeekly(995.0)
                .xpMonthly(995.0)
                .xpEarnedAsCreator(40L)
                .totalCooksOfYourRecipes(3L)
                .weeklyCreatorCooks(1L)
                .weeklyCreatorXp(40L)
                .badges(List.of("existing-badge"))
                .badgeTimestamps(new HashMap<>())
                .recipeCookCounts(new HashMap<>())
                .recipesCooked(0L)
                .recipesMastered(0L)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId("creator-1")
                .displayName("Chef Creator")
                .statistics(stats)
                .build();

        when(userProfileRepository.findByUserId("creator-1")).thenReturn(Optional.of(profile));
        when(listenerIdempotencyService.tryProcess(anyString(), eq("gamification-delivery"))).thenReturn(true);
        when(listenerIdempotencyService.tryProcess(anyString(), eq("reminder-delivery"))).thenReturn(true);
        when(notificationPreferencesProvider.isNotificationEnabled("creator-1", "xpAndLevelUps")).thenReturn(true);
        when(notificationPreferencesProvider.isNotificationEnabled("creator-1", "social")).thenReturn(true);

        statisticsService.applyCreatorReward("creator-1", 10.0);

        ArgumentCaptor<GamificationNotificationEvent> gamificationCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
        verify(kafkaTemplate).send(eq("gamification-delivery"), gamificationCaptor.capture());
        GamificationNotificationEvent gamificationEvent = gamificationCaptor.getValue();

        ArgumentCaptor<ReminderEvent> reminderCaptor = ArgumentCaptor.forClass(ReminderEvent.class);
        verify(kafkaTemplate).send(eq("reminder-delivery"), reminderCaptor.capture());
        ReminderEvent reminderEvent = reminderCaptor.getValue();

        bellNotificationListener.listenGamificationDelivery(gamificationEvent);
        bellNotificationListener.listenReminderDelivery(reminderEvent);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        assertThat(gamificationEvent.getSource()).isEqualTo("CREATOR_BONUS");
        assertThat(gamificationEvent.isLeveledUp()).isTrue();
        assertThat(reminderEvent.getReminderType()).isEqualTo("CREATOR_BONUS");
        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getType)
                .containsExactly(NotificationType.LEVEL_UP, NotificationType.CREATOR_BONUS);
        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getType)
                .doesNotContain(NotificationType.XP_AWARDED);
        assertThat(notificationCaptor.getAllValues().get(1).getContent()).contains("Someone cooked your recipe");
        verify(listenerIdempotencyService).tryProcess(gamificationEvent.getEventId(), "gamification-delivery");
        verify(listenerIdempotencyService).tryProcess(reminderEvent.getEventId(), "reminder-delivery");
    }

        @Test
        void duplicateLinkingPostGamificationEventIsDroppedBeforeSecondNotificationWrite() {
                Statistics stats = Statistics.builder()
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
                UserProfile profile = UserProfile.builder()
                                .userId("user-1")
                                .displayName("Chef Kix")
                                .statistics(stats)
                                .build();

                when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));
                when(listenerIdempotencyService.tryProcess(anyString(), eq("gamification-delivery"))).thenReturn(true, false);
                when(notificationPreferencesProvider.isNotificationEnabled("user-1", "xpAndLevelUps")).thenReturn(true);

                statisticsService.rewardXpFull(
                                "user-1",
                                55.0,
                                List.of(),
                                false,
                                "recipe-1",
                                "LINKING_POST",
                                "session-1");

                ArgumentCaptor<GamificationNotificationEvent> eventCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
                verify(kafkaTemplate).send(eq("gamification-delivery"), eventCaptor.capture());
                GamificationNotificationEvent event = eventCaptor.getValue();

                bellNotificationListener.listenGamificationDelivery(event);
                bellNotificationListener.listenGamificationDelivery(event);

                verify(notificationRepository).save(any(Notification.class));
                verify(listenerIdempotencyService, times(2)).tryProcess(event.getEventId(), "gamification-delivery");
        }

            @Test
            void failedLinkingPostNotificationWriteReleasesIdempotencyAndSucceedsOnRedelivery() {
                Statistics stats = Statistics.builder()
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
                UserProfile profile = UserProfile.builder()
                        .userId("user-1")
                        .displayName("Chef Kix")
                        .statistics(stats)
                        .build();

                when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));
                when(listenerIdempotencyService.tryProcess(anyString(), eq("gamification-delivery"))).thenReturn(true, true);
                when(notificationPreferencesProvider.isNotificationEnabled("user-1", "xpAndLevelUps")).thenReturn(true);
                when(notificationRepository.save(any(Notification.class)))
                        .thenThrow(new RuntimeException("mongo down"))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                statisticsService.rewardXpFull(
                        "user-1",
                        55.0,
                        List.of(),
                        false,
                        "recipe-1",
                        "LINKING_POST",
                        "session-1");

                ArgumentCaptor<GamificationNotificationEvent> eventCaptor = ArgumentCaptor.forClass(GamificationNotificationEvent.class);
                verify(kafkaTemplate).send(eq("gamification-delivery"), eventCaptor.capture());
                GamificationNotificationEvent event = eventCaptor.getValue();

                assertThrows(RuntimeException.class, () -> bellNotificationListener.listenGamificationDelivery(event));

                bellNotificationListener.listenGamificationDelivery(event);

                verify(notificationRepository, times(2)).save(any(Notification.class));
                verify(listenerIdempotencyService).removeProcessed(event.getEventId(), "gamification-delivery");
                verify(listenerIdempotencyService, times(2)).tryProcess(event.getEventId(), "gamification-delivery");
            }
}