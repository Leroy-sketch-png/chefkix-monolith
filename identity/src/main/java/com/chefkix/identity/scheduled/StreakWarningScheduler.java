package com.chefkix.identity.scheduled;

import com.chefkix.shared.event.ReminderEvent;
import com.chefkix.identity.entity.UserProfile;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to send streak warning notifications.
 *
 * <p>Per spec (04-statistics.txt): - Cooking streak uses 72-hour window - Users should be warned
 * before their streak expires - High priority notification: "Cook today to keep your X-day streak!"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakWarningScheduler {

  private final MongoTemplate mongoTemplate;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  private static final String REMINDER_TOPIC = "reminder-delivery";

  // Warning thresholds (hours before streak expires)
  private static final int FIRST_WARNING_HOURS = 12; // 60 hours into 72-hour window
  private static final int FINAL_WARNING_HOURS = 6; // 66 hours into 72-hour window

  /**
   * Run every hour to check for users with expiring streaks. Streak expires after 72 hours from
   * last cook.
   */
  @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
  public void checkExpiringStreaks() {
    try {
      log.info("Running streak warning check...");

      Instant now = Instant.now();

      // Find users whose lastCookAt is between (now - 72h + warningHours) and (now - 72h +
      // warningHours + 1h)
      // This means they have `warningHours` left before their 72h window expires

      // First warning: 12 hours left (so they cooked ~60 hours ago)
      sendWarningsForTimeWindow(now, FIRST_WARNING_HOURS, "NORMAL");

      // Final warning: 6 hours left (so they cooked ~66 hours ago)
      sendWarningsForTimeWindow(now, FINAL_WARNING_HOURS, "HIGH");
    } catch (Exception e) {
      log.error("Streak warning scheduler failed — will retry next cycle", e);
    }
  }

  private void sendWarningsForTimeWindow(Instant now, int hoursRemaining, String priorityStr) {
    // Calculate the time window:
    // If hoursRemaining = 12, we want users who cooked between 59-60 hours ago
    // (so they have 12-13 hours left in their 72h window)
    int hoursAgoMin = 72 - hoursRemaining;
    int hoursAgoMax = hoursAgoMin + 1;

    Instant windowStart = now.minus(hoursAgoMax, ChronoUnit.HOURS);
    Instant windowEnd = now.minus(hoursAgoMin, ChronoUnit.HOURS);

    Query query = new Query();
    query.addCriteria(Criteria.where("statistics.streakCount").gt(0));
    query.addCriteria(Criteria.where("statistics.lastCookAt").gte(windowStart).lt(windowEnd));

    List<UserProfile> profiles = mongoTemplate.find(query, UserProfile.class);

    log.info("Found {} users with streaks expiring in {} hours", profiles.size(), hoursRemaining);

    for (UserProfile profile : profiles) {
      sendStreakWarning(profile, hoursRemaining, priorityStr);
    }
  }

  private void sendStreakWarning(UserProfile profile, int hoursRemaining, String priorityStr) {
    if (profile.getStatistics() == null) {
      log.warn("Skipping streak warning for user {} — Statistics is null", profile.getUserId());
      return;
    }
    int streakCount = profile.getStatistics().getStreakCount();

    String content =
        hoursRemaining <= 6
            ? String.format(
                "⚠️ Cook in the next %d hours to keep your %d-day streak!",
                hoursRemaining, streakCount)
            : String.format(
                "🔥 Cook today to keep your %d-day streak! %d hours remaining.",
                streakCount, hoursRemaining);

    ReminderEvent event =
        ReminderEvent.builder()
            .userId(profile.getUserId())
            .displayName(profile.getDisplayName())
            .reminderType("STREAK_WARNING")
            .content(content)
            .priority(ReminderEvent.ReminderPriority.valueOf(priorityStr))
            .streakCount(streakCount)
            .hoursRemaining(hoursRemaining)
            .build();

    kafkaTemplate.send(REMINDER_TOPIC, event);

    log.info(
        "Sent streak warning to user {} (streak: {}, hours left: {})",
        profile.getUserId(),
        streakCount,
        hoursRemaining);
  }
}
