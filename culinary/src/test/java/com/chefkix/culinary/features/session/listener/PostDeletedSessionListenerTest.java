package com.chefkix.culinary.features.session.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostDeletedSessionListenerTest {

    @Mock
    private CookingSessionService cookingSessionService;
    @Mock
    private KafkaIdempotencyService idempotencyService;

    @InjectMocks
    private PostDeletedSessionListener listener;

        @Test
        void listenPostDeletedDeliverySkipsNullEvent() {
                listener.listenPostDeletedDelivery(null);

                verify(idempotencyService, never()).tryProcess("post-1", "post-deleted-delivery:culinary-sessions");
                verify(cookingSessionService, never()).markLinkedPostDeleted("post-1");
        }

        @Test
        void listenPostDeletedDeliverySkipsEventWithMissingEventId() {
                PostDeletedEvent event = PostDeletedEvent.builder()
                                .userId("user-1")
                                .postId("post-1")
                                .build();
                event.setEventId(" ");

                listener.listenPostDeletedDelivery(event);

                verify(idempotencyService, never()).tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions");
                verify(cookingSessionService, never()).markLinkedPostDeleted("post-1");
        }

        @Test
        void listenPostDeletedDeliverySkipsDuplicateEvent() {
                PostDeletedEvent event = PostDeletedEvent.builder()
                                .userId("user-1")
                                .postId("post-1")
                                .build();

                when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions"))
                                .thenReturn(false);

                listener.listenPostDeletedDelivery(event);

                verify(cookingSessionService, never()).markLinkedPostDeleted("post-1");
        }

    @Test
    void listenPostDeletedDeliveryReconcilesLinkedSessions() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions"))
                .thenReturn(true);
        when(cookingSessionService.markLinkedPostDeleted("post-1")).thenReturn(1);

        listener.listenPostDeletedDelivery(event);

        verify(cookingSessionService).markLinkedPostDeleted("post-1");
    }

    @Test
    void listenPostDeletedDeliveryReleasesIdempotencyOnFailure() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions"))
                .thenReturn(true);
        RuntimeException failure = new RuntimeException("session reconciliation failed");
        doThrow(failure)
                .when(cookingSessionService)
                .markLinkedPostDeleted("post-1");

        assertThrows(RuntimeException.class, () -> listener.listenPostDeletedDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), "post-deleted-delivery:culinary-sessions");
    }

    @Test
    void listenPostDeletedDeliverySkipsBlankPostId() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();
        event.setPostId(" ");

        listener.listenPostDeletedDelivery(event);

        verify(idempotencyService, never()).tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions");
        verify(cookingSessionService, never()).markLinkedPostDeleted("post-1");
    }
}