package com.chefkix.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.api.NotificationPreferencesProvider;
import com.chefkix.notification.dto.response.NotificationResponse;
import com.chefkix.notification.entity.Notification;
import com.chefkix.notification.enums.NotificationType;
import com.chefkix.notification.mapper.NotificationMapper;
import com.chefkix.notification.repository.NotificationRepository;
import com.chefkix.shared.event.GamificationNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

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
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(notificationMapper.toNotificationResponse(any(Notification.class))).thenAnswer(invocation -> {
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
    void handleGamificationEventCreatesXpAwardedNotificationForPlainXpEvent() {
        when(notificationPreferencesProvider.isNotificationEnabled("user-1", "xpAndLevelUps")).thenReturn(true);

        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .displayName("Chef Kix")
                .xpEarned(70.0)
                .totalXp(170.0)
                .leveledUp(false)
                .previousLevel(1)
                .newLevel(1)
                .newBadges(java.util.List.of())
                .source("LINKING_POST")
                .recipeId("recipe-1")
                .sessionId("session-1")
                .build();

        notificationService.handleGamificationEvent(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.XP_AWARDED);
        assertThat(notification.getTargetEntityId()).isEqualTo("session-1");
        assertThat(notification.getContent()).contains("70 XP");
        verify(messagingTemplate).convertAndSend(eq("/topic/user/user-1"), anyMap());
    }

    @Test
    void handleGamificationEventSkipsXpAwardedForCreatorBonusSource() {
        when(notificationPreferencesProvider.isNotificationEnabled("creator-1", "xpAndLevelUps")).thenReturn(true);

        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("creator-1")
                .displayName("Chef Creator")
                .xpEarned(5.0)
                .totalXp(105.0)
                .leveledUp(false)
                .previousLevel(1)
                .newLevel(1)
                .newBadges(java.util.List.of())
                .source("CREATOR_BONUS")
                .recipeId("recipe-1")
                .sessionId("session-1")
                .build();

        notificationService.handleGamificationEvent(event);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), anyMap());
    }

    @Test
    void handleGamificationEventOrdersLevelAndBadgeNotificationsWithoutRedundantXpRow() {
        when(notificationPreferencesProvider.isNotificationEnabled("user-1", "xpAndLevelUps")).thenReturn(true);
        when(notificationPreferencesProvider.isNotificationEnabled("user-1", "badges")).thenReturn(true);

        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .displayName("Chef Kix")
                .xpEarned(120.0)
                .totalXp(420.0)
                .leveledUp(true)
                .previousLevel(3)
                .newLevel(4)
                .newTitle("Broth Boss")
                .newBadges(java.util.List.of("First Ramen", "Night Cook"))
                .source("COOKING_SESSION")
                .recipeId("recipe-1")
                .sessionId("session-1")
                .build();

        notificationService.handleGamificationEvent(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getType)
                .containsExactly(NotificationType.LEVEL_UP, NotificationType.BADGE_EARNED);
        assertThat(notificationCaptor.getAllValues().get(0).getContent()).contains("Level 4");
        assertThat(notificationCaptor.getAllValues().get(1).getContent()).contains("First Ramen", "Night Cook");
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/user/user-1"), anyMap());
    }
}