package com.chefkix.identity.listener;

import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.identity.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeleteListener {

  private final StatisticsService statisticsService;

  @KafkaListener(
      topics = "post-deleted-delivery",
      groupId = "post-deleted-group",
      containerFactory = "postDeletedKafkaListenerContainerFactory")
  public void listenPostDeletedDelivery(PostDeletedEvent event) {
    statisticsService.incrementCounter(event.getUserId(), "totalRecipesPublished", -1);
  }
}
