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
  private static final String XP_IDEMPOTENCY_SCOPE = "xp-delivery";

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
    if (event.getUserId() == null || event.getUserId().isBlank()) {
      log.error("Received XP event with null/blank userId, skipping. eventId={}", event.getEventId());
      return;
    }
    if (event.getAmount() < 0) {
      throw new IllegalArgumentException(
          "XP event has negative amount=" + event.getAmount() + " for userId=" + event.getUserId());
    }
    if (event.getAmount() == 0 && (event.getBadges() == null || event.getBadges().isEmpty()) && !event.isChallengeCompleted()) {
      log.warn("Received zero-XP event with no badges and no challenge for userId={}, skipping", event.getUserId());
      return;
    }

    if (!idempotencyService.tryProcess(event.getEventId(), XP_IDEMPOTENCY_SCOPE)) {
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
      String source = event.getSource();
      if ("CREATOR_BONUS".equals(source)) {
        statisticsService.applyCreatorReward(event.getUserId(), event.getAmount());
      } else if (source != null && source.startsWith("SOCIAL_")) {
        statisticsService.rewardSocialXp(event.getUserId(), event.getAmount(), source);
      } else {
        statisticsService.rewardXpFull(
        event.getUserId(),
        event.getAmount(),
        event.getBadges(),
        event.isChallengeCompleted(),
        event.getRecipeId(),
        event.getSource(),
        event.getSessionId());
      }
    } catch (AppException e) {
      if (e.getErrorCode() == ErrorCode.USER_NOT_FOUND || e.getErrorCode() == ErrorCode.PROFILE_NOT_FOUND) {
        log.error(
            "Skipping unrecoverable XP event due to missing user profile: userId={}, eventId={}, source={}",
            event.getUserId(),
            event.getEventId(),
            event.getSource(),
            e);
        return;
      }
      idempotencyService.removeProcessed(event.getEventId(), XP_IDEMPOTENCY_SCOPE);
      throw e;
    } catch (Exception e) {
      idempotencyService.removeProcessed(event.getEventId(), XP_IDEMPOTENCY_SCOPE);
      log.error("Failed to process XP event: userId={}, amount={}, source={}, eventId={}",
          event.getUserId(), event.getAmount(), event.getSource(), event.getEventId(), e);
throw e;
    }
  }
}
