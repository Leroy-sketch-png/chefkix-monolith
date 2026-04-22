package com.chefkix.notification.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.GamificationNotificationEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BellNotificationListenerTest {

    private static final String GAMIFICATION_SCOPE = "gamification-delivery";

    @Mock
    private NotificationService notificationService;
    @Mock
    private KafkaIdempotencyService idempotencyService;

    private BellNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new BellNotificationListener(notificationService, idempotencyService);
    }

    @Test
    void listenGamificationDeliverySkipsDuplicateEvent() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(false);

        listener.listenGamificationDelivery(event);

        verify(notificationService, never()).handleGamificationEvent(event);
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    @Test
    void listenGamificationDeliverySkipsInvalidBlankUserId() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("   ")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(true);

        listener.listenGamificationDelivery(event);

        verify(notificationService, never()).handleGamificationEvent(event);
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    @Test
    void listenGamificationDeliveryRoutesValidEvent() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .newBadges(List.of())
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(true);

        listener.listenGamificationDelivery(event);

        verify(notificationService).handleGamificationEvent(event);
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    @Test
    void listenGamificationDeliveryReleasesIdempotencyOnFailure() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(true);
        RuntimeException failure = new RuntimeException("notification save failed");
        org.mockito.Mockito.doThrow(failure)
                .when(notificationService)
                .handleGamificationEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenGamificationDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }
}