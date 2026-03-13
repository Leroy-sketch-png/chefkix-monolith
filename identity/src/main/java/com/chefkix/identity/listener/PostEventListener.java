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
    if (!idempotencyService.tryProcess(event.getEventId(), "post-delivery")) {
      return;
    }
    statisticsService.incrementCounter(event.getUserId(), "totalRecipesPublished", 1);
  }
}
