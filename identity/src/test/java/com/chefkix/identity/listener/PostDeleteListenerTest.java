package com.chefkix.identity.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.service.StatisticsCounterOperations;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostDeleteListenerTest {

    @Mock
        private StatisticsCounterOperations statisticsService;
        @Mock
    private KafkaIdempotencyService idempotencyService;

        private PostDeleteListener listener;

        @BeforeEach
        void setUp() {
                listener = new PostDeleteListener(statisticsService, idempotencyService);
        }

    @Test
        void listenPostDeletedDeliverySkipsNullEvent() {
                listener.listenPostDeletedDelivery(null);

                verify(idempotencyService, never()).tryProcess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
                verify(statisticsService, never()).incrementCounter(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
    void listenPostDeletedDeliveryDecrementsPublishedCount() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:identity"))
                .thenReturn(true);

        listener.listenPostDeletedDelivery(event);

        verify(statisticsService).incrementCounter("user-1", "totalRecipesPublished", -1);
    }

        @Test
        void listenPostDeletedDeliverySkipsDuplicateEvent() {
                PostDeletedEvent event = PostDeletedEvent.builder()
                                .userId("user-1")
                                .postId("post-1")
                                .build();

                when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:identity"))
                                .thenReturn(false);

                listener.listenPostDeletedDelivery(event);

                verify(statisticsService, never()).incrementCounter("user-1", "totalRecipesPublished", -1);
        }

        @Test
        void listenPostDeletedDeliverySkipsEventWithMissingEventId() {
                PostDeletedEvent event = PostDeletedEvent.builder()
                                .userId("user-1")
                                .postId("post-1")
                                .build();
                event.setEventId(" ");

                listener.listenPostDeletedDelivery(event);

                verify(idempotencyService, never()).tryProcess(event.getEventId(), "post-deleted-delivery:identity");
                verify(statisticsService, never()).incrementCounter("user-1", "totalRecipesPublished", -1);
        }

        @Test
        void listenPostDeletedDeliverySkipsEventWithBlankUserId() {
                PostDeletedEvent event = PostDeletedEvent.builder()
                                .userId("user-1")
                                .postId("post-1")
                                .build();
                event.setUserId(" ");

                listener.listenPostDeletedDelivery(event);

                verify(idempotencyService, never()).tryProcess(event.getEventId(), "post-deleted-delivery:identity");
                verify(statisticsService, never()).incrementCounter(" ", "totalRecipesPublished", -1);
        }

    @Test
    void listenPostDeletedDeliveryReleasesIdempotencyOnFailure() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:identity"))
                .thenReturn(true);
        RuntimeException failure = new RuntimeException("stats write failed");
        doThrow(failure)
                .when(statisticsService)
                .incrementCounter("user-1", "totalRecipesPublished", -1);

        assertThrows(RuntimeException.class, () -> listener.listenPostDeletedDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), "post-deleted-delivery:identity");
    }
}