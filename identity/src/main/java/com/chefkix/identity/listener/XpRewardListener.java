package com.chefkix.identity.listener;

import com.chefkix.shared.service.KafkaIdempotencyService;
import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
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
  private final KafkaIdempotencyService idempotencyService;

  @KafkaListener(
      topics = "xp-delivery",
      groupId = "xp-rewarded-group",
      containerFactory = "xpRewardedKafkaListenerContainerFactory")
  public void listenXpRewardDelivery(XpRewardEvent event) {
    if (event == null) {
      log.error("Received null XP event, skipping");
      return;
    }
    if (event.getEventId() == null || event.getEventId().isBlank()) {
      log.error("Received XP event with missing eventId, skipping. userId={}", event.getUserId());
      return;
    }
    // Validate required fields before processing
    if (event.getUserId() == null || event.getUserId().isBlank()) {
      log.error("Received XP event with null/blank userId, skipping. eventId={}", event.getEventId());
      return;
    }
    if (event.getAmount() <= 0) {
      log.error("Received XP event with invalid amount={}, skipping. userId={}", event.getAmount(), event.getUserId());
      return;
    }

    // Idempotency check: prevent duplicate XP awards on Kafka redelivery
    if (!idempotencyService.tryProcess(event.getEventId(), "xp-delivery")) {
      return;
    }

    log.info(
        "Received XP event: userId={}, amount={}, source={}, badges={}, challengeCompleted={}",
        event.getUserId(),
        event.getAmount(),
        event.getSource(),
        event.getBadges(),
        event.isChallengeCompleted());

    try {
      if ("CREATOR_BONUS".equals(event.getSource())) {
        // CASE 1: Creator bonus (no badges, no streak update)
        statisticsService.applyCreatorReward(event.getUserId(), event.getAmount());
      } else {
        // CASE 2: Cook XP (with badges, streak update, and possibly challenge streak)
        statisticsService.rewardXpFull(
            event.getUserId(), event.getAmount(), event.getBadges(), event.isChallengeCompleted());
      }
    } catch (AppException e) {
      if (e.getErrorCode() == ErrorCode.USER_NOT_FOUND || e.getErrorCode() == ErrorCode.PROFILE_NOT_FOUND) {
        // Permanent data error: retrying this message will not heal missing users/profiles.
        log.error(
            "Skipping unrecoverable XP event due to missing user profile: userId={}, eventId={}, source={}",
            event.getUserId(),
            event.getEventId(),
            event.getSource(),
            e);
        return;
      }
      throw e;
    } catch (Exception e) {
      log.error("Failed to process XP event: userId={}, amount={}, source={}, eventId={}",
          event.getUserId(), event.getAmount(), event.getSource(), event.getEventId(), e);
      throw e; // Re-throw so Kafka error handler can retry
    }
  }
}
