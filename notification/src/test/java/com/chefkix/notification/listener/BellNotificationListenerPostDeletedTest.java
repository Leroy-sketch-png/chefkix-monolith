package com.chefkix.notification.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.CommentEvent;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BellNotificationListenerPostDeletedTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private KafkaIdempotencyService idempotencyService;

    @InjectMocks
    private BellNotificationListener listener;

    @Test
    void listenPostDeletedDeliverySkipsNullEvent() {
        listener.listenPostDeletedDelivery(null);

        verify(idempotencyService, never()).tryProcess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(notificationService, never()).cleanupDeletedPostNotifications(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listenPostDeletedDeliverySkipsEventWithMissingEventId() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();
        event.setEventId(" ");

        listener.listenPostDeletedDelivery(event);

        verify(idempotencyService, never()).tryProcess(event.getEventId(), "post-deleted-delivery:notification");
        verify(notificationService, never()).cleanupDeletedPostNotifications("post-1");
    }

    @Test
    void listenPostDeletedDeliveryDeletesPostScopedNotifications() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification")).thenReturn(true);

        listener.listenPostDeletedDelivery(event);

        verify(notificationService).cleanupDeletedPostNotifications("post-1");
    }

    @Test
    void listenPostDeletedDeliverySkipsUnexpectedEventType() {
        CommentEvent event = CommentEvent.builder()
                .postId("post-1")
                .postOwnerId("owner-1")
                .commenterId("commenter-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification")).thenReturn(true);

        listener.listenPostDeletedDelivery(event);

        verify(notificationService, never()).cleanupDeletedPostNotifications("post-1");
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), "post-deleted-delivery:notification");
    }

    @Test
    void listenPostDeletedDeliverySkipsDuplicateEvent() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification")).thenReturn(false);

        listener.listenPostDeletedDelivery(event);

        verify(notificationService, never()).cleanupDeletedPostNotifications("post-1");
    }

    @Test
    void listenPostDeletedDeliverySkipsBlankPostId() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();
        event.setPostId(" ");

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification")).thenReturn(true);

        listener.listenPostDeletedDelivery(event);

        verify(notificationService, never()).cleanupDeletedPostNotifications("post-1");
    }

    @Test
    void listenPostDeletedDeliveryReleasesIdempotencyOnCleanupFailure() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification")).thenReturn(true);
        RuntimeException failure = new RuntimeException("notification cleanup failed");
        doThrow(failure)
                .when(notificationService)
                .cleanupDeletedPostNotifications("post-1");

        assertThrows(RuntimeException.class, () -> listener.listenPostDeletedDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), "post-deleted-delivery:notification");
    }
}