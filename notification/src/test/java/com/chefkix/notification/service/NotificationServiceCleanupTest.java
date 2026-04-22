package com.chefkix.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.notification.entity.Notification;
import com.chefkix.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceCleanupTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                null,
                null,
                pushNotificationService,
                null,
                null);
    }

    @Test
    void cleanupDeletedUserStateDeletesRecipientAndActorLinkedNotificationsAndPushTokens() {
        String userId = "user-1";
        Notification actorNotificationOne = Notification.builder()
                .id("notif-1")
                .recipientId("user-2")
                .latestActorId(userId)
                .actorIds(Set.of(userId))
                .build();
        Notification actorNotificationTwo = Notification.builder()
                .id("notif-2")
                .recipientId("user-3")
                .latestActorId("user-9")
                .actorIds(Set.of(userId, "user-9"))
                .build();

        when(notificationRepository.deleteAllByRecipientId(userId)).thenReturn(3L);
        when(notificationRepository.findAllByLatestActorIdOrActorIdsContaining(userId, userId))
                .thenReturn(List.of(actorNotificationOne, actorNotificationTwo));
        when(pushNotificationService.cleanupDeletedUserTokens(userId)).thenReturn(4L);

        long affectedRecords = notificationService.cleanupDeletedUserState(userId);

        assertThat(affectedRecords).isEqualTo(9L);
        verify(notificationRepository).deleteAllByRecipientId(userId);
        verify(notificationRepository).deleteAll(List.of(actorNotificationOne, actorNotificationTwo));
        verify(pushNotificationService).cleanupDeletedUserTokens(userId);
    }
}