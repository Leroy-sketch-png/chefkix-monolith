package com.chefkix.identity.listener;

import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.identity.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class XpRewardListener {
  private final StatisticsService statisticsService;

  @KafkaListener(
      topics = "xp-delivery",
      groupId = "xp-rewarded-group",
      containerFactory = "xpRewardedKafkaListenerContainerFactory")
  public void listenXpRewardDelivery(XpRewardEvent event) {
    log.info(
        "Received XP event: userId={}, amount={}, source={}, badges={}, challengeCompleted={}",
        event.getUserId(),
        event.getAmount(),
        event.getSource(),
        event.getBadges(),
        event.isChallengeCompleted());

    if ("CREATOR_BONUS".equals(event.getSource())) {
      // CASE 1: Creator bonus (no badges, no streak update)
      statisticsService.applyCreatorReward(event.getUserId(), event.getAmount());

    } else {
      // CASE 2: Cook XP (with badges, streak update, and possibly challenge streak)
      statisticsService.rewardXpFull(
          event.getUserId(), event.getAmount(), event.getBadges(), event.isChallengeCompleted());
    }
  }
}
