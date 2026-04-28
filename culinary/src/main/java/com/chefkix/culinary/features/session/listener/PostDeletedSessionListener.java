package com.chefkix.culinary.features.session.listener;

import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeletedSessionListener {

    private static final String IDEMPOTENCY_SCOPE = "post-deleted-delivery:culinary-sessions";

    private final CookingSessionService cookingSessionService;
    private final KafkaIdempotencyService idempotencyService;

    @KafkaListener(
            topics = "post-deleted-delivery",
            groupId = "culinary-post-deleted-group",
            containerFactory = "postDeletedKafkaListenerContainerFactory")
    public void listenPostDeletedDelivery(PostDeletedEvent event) {
        if (event == null) {
            log.error("Received null PostDeletedEvent, skipping");
            return;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            log.error("PostDeletedEvent with missing eventId, skipping. postId={}", event.getPostId());
            return;
        }
        if (event.getPostId() == null || event.getPostId().isBlank()) {
            log.error("PostDeletedEvent with null/blank postId, eventId={}", event.getEventId());
            return;
        }
        if (!idempotencyService.tryProcess(event.getEventId(), IDEMPOTENCY_SCOPE)) {
            return;
        }
        try {
            int updatedSessions = cookingSessionService.markLinkedPostDeleted(event.getPostId());
            log.info("Reconciled {} cooking sessions for deleted post {}", updatedSessions, event.getPostId());
        } catch (Exception e) {
            idempotencyService.removeProcessed(event.getEventId(), IDEMPOTENCY_SCOPE);
            throw e;
        }
    }
}