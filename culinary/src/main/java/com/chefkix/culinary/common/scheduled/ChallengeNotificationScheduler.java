package com.chefkix.culinary.common.scheduled;

import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.challenge.service.ChallengePoolService;
import com.chefkix.shared.event.ReminderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Sends daily challenge notifications via Kafka reminder-delivery topic.
 * <p>
 * - CHALLENGE_AVAILABLE (00:05 UTC): Notify active users about the new daily challenge.
 * - CHALLENGE_REMINDER  (20:00 UTC): Remind users who haven't completed today's challenge.
 * <p>
 * Follows the same pattern as {@link PostDeadlineScheduler} and
 * {@code com.chefkix.identity.scheduled.StreakWarningScheduler}.
 * <p>
 * Queries the {@code user_profiles} collection directly via MongoTemplate (same DB)
 * to avoid cross-module entity dependencies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeNotificationScheduler {

    private final ChallengePoolService challengePoolService;
    private final ChallengeLogRepository challengeLogRepository;
    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REMINDER_TOPIC = "reminder-delivery";
    private static final String USER_PROFILES_COLLECTION = "user_profiles";
    private static final int ACTIVE_DAYS_THRESHOLD = 7;

    /**
     * Daily at 00:05 UTC — notify active users about the new daily challenge.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void sendChallengeAvailableNotifications() {
        try {
            ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
            if (challenge == null) {
                log.warn("No challenge available for today, skipping CHALLENGE_AVAILABLE notifications");
                return;
            }

            List<Document> activeUsers = findActiveUsers();
            log.info("Sending CHALLENGE_AVAILABLE to {} active users for: {}", activeUsers.size(), challenge.getTitle());

            int sent = 0;
            for (Document user : activeUsers) {
                String userId = user.getString("userId");
                String displayName = user.getString("displayName");
                if (userId == null) continue;

                ReminderEvent event = ReminderEvent.builder()
                        .userId(userId)
                        .displayName(displayName != null ? displayName : "Chef")
                        .reminderType("CHALLENGE_AVAILABLE")
                        .content(String.format(
                                "\uD83C\uDFC6 New daily challenge: %s — %s (Bonus: +%d XP)",
                                challenge.getTitle(), challenge.getDescription(), challenge.getBonusXp()))
                        .priority(ReminderEvent.ReminderPriority.NORMAL)
                        .challengeCategory(challenge.getId())
                        .build();

                kafkaTemplate.send(REMINDER_TOPIC, event);
                sent++;
            }

            log.info("CHALLENGE_AVAILABLE notifications sent to {} users", sent);
        } catch (Exception e) {
            log.error("Challenge available notification scheduler failed — will retry next cycle", e);
        }
    }

    /**
     * Daily at 20:00 UTC — remind active users who haven't completed today's challenge.
     * Only 4 hours remain until midnight reset.
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendChallengeReminderNotifications() {
        try {
            ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
            if (challenge == null) {
                log.warn("No challenge available for today, skipping CHALLENGE_REMINDER notifications");
                return;
            }

            String todayStr = LocalDate.now(ZoneId.of("UTC")).toString();
            List<Document> activeUsers = findActiveUsers();

            int sent = 0;
            for (Document user : activeUsers) {
                String userId = user.getString("userId");
                String displayName = user.getString("displayName");
                if (userId == null) continue;

                // Skip users who already completed today's challenge
                if (challengeLogRepository.existsByUserIdAndChallengeDate(userId, todayStr)) {
                    continue;
                }

                ReminderEvent event = ReminderEvent.builder()
                        .userId(userId)
                        .displayName(displayName != null ? displayName : "Chef")
                        .reminderType("CHALLENGE_REMINDER")
                        .content(String.format(
                                "\u23F0 Don't forget today's challenge: %s — Only 4 hours left!",
                                challenge.getTitle()))
                        .priority(ReminderEvent.ReminderPriority.HIGH)
                        .hoursRemaining(4)
                        .challengeCategory(challenge.getId())
                        .build();

                kafkaTemplate.send(REMINDER_TOPIC, event);
                sent++;
            }

            log.info("CHALLENGE_REMINDER sent to {} users (out of {} active)", sent, activeUsers.size());
        } catch (Exception e) {
            log.error("Challenge reminder notification scheduler failed — will retry next cycle", e);
        }
    }

    /**
     * Finds users who have been active in the last {@value ACTIVE_DAYS_THRESHOLD} days.
     * Uses direct MongoTemplate query on user_profiles collection to avoid
     * cross-module entity dependency on identity module.
     */
    private List<Document> findActiveUsers() {
        Instant threshold = Instant.now().minus(ACTIVE_DAYS_THRESHOLD, ChronoUnit.DAYS);

        Query query = new Query();
        query.addCriteria(Criteria.where("statistics.lastCookAt").gte(threshold));
        query.fields().include("userId").include("displayName");

        return mongoTemplate.find(query, Document.class, USER_PROFILES_COLLECTION);
    }
}
