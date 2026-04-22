package com.chefkix.identity.listener;

import com.chefkix.shared.service.KafkaIdempotencyService;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.identity.service.StatisticsService;
import com.chefkix.identity.service.StatisticsCounterOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeleteListener {

  private static final String POST_DELETED_IDEMPOTENCY_SCOPE = "post-deleted-delivery:identity";

  private final StatisticsCounterOperations statisticsService;
  private final KafkaIdempotencyService idempotencyService;

  @KafkaListener(
      topics = "post-deleted-delivery",
      groupId = "post-deleted-group",
      containerFactory = "postDeletedKafkaListenerContainerFactory")
  public void listenPostDeletedDelivery(PostDeletedEvent event) {
    if (event == null) {
      log.error("Received null PostDeletedEvent, skipping");
      return;
    }
    if (event.getEventId() == null || event.getEventId().isBlank()) {
      log.error("PostDeletedEvent with missing eventId, skipping. userId={}", event.getUserId());
      return;
    }
    if (event.getUserId() == null || event.getUserId().isBlank()) {
      log.error("PostDeletedEvent with null/blank userId, eventId={}", event.getEventId());
      return;
    }
    if (!idempotencyService.tryProcess(event.getEventId(), POST_DELETED_IDEMPOTENCY_SCOPE)) {
      return;
    }
    try {
      statisticsService.incrementCounter(event.getUserId(), "totalRecipesPublished", -1);
    } catch (Exception e) {
      idempotencyService.removeProcessed(event.getEventId(), POST_DELETED_IDEMPOTENCY_SCOPE);
      log.error("Failed to process PostDeletedEvent for userId={}, eventId={}: {}", event.getUserId(), event.getEventId(), e.getMessage(), e);
      throw e;
    }
  }
}
