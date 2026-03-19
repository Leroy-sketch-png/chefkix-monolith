package com.chefkix.identity.listener;

import com.chefkix.shared.service.KafkaIdempotencyService;
import com.chefkix.shared.event.PostCreatedEvent;
import com.chefkix.identity.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

  private final StatisticsService statisticsService;
  private final KafkaIdempotencyService idempotencyService;

  @KafkaListener(
      topics = "post-delivery",
      groupId = "post-created-group",
      containerFactory = "postCreatedKafkaListenerContainerFactory")
  public void listenPostCreatedDelivery(PostCreatedEvent event) {
    if (event == null) {
      log.error("Received null PostCreatedEvent, skipping");
      return;
    }
    if (event.getEventId() == null || event.getEventId().isBlank()) {
      log.error("Received PostCreatedEvent with missing eventId, skipping. userId={}", event.getUserId());
      return;
    }
    if (event.getUserId() == null || event.getUserId().isBlank()) {
      log.error("Received PostCreatedEvent with null/blank userId, skipping. eventId={}", event.getEventId());
      return;
    }
    if (!idempotencyService.tryProcess(event.getEventId(), "post-delivery")) {
      return;
    }
    try {
      statisticsService.incrementCounter(event.getUserId(), "totalRecipesPublished", 1);
    } catch (Exception e) {
      log.error("Failed to process PostCreatedEvent for userId={}, eventId={}: {}", event.getUserId(), event.getEventId(), e.getMessage(), e);
    }
  }
}
