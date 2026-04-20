package com.chefkix.culinary.common.scheduled;

import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.challenge.repository.SeasonalChallengeRepository;
import com.chefkix.culinary.features.challenge.service.ChallengePoolService;
import com.chefkix.culinary.features.challenge.entity.SeasonalChallenge;
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
import java.util.Set;

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
    private final SeasonalChallengeRepository seasonalChallengeRepository;
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

            // Batch query: get all userIds who already completed today's challenge
            List<String> allUserIds = activeUsers.stream()
                    .map(u -> u.getString("userId"))
                    .filter(id -> id != null)
                    .toList();
            Set<String> completedUserIds = new java.util.HashSet<>(
                    challengeLogRepository.findUserIdsWithChallengeDate(todayStr, allUserIds));

            int sent = 0;
            for (Document user : activeUsers) {
                String userId = user.getString("userId");
                String displayName = user.getString("displayName");
                if (userId == null) continue;

                // Skip users who already completed today's challenge
                if (completedUserIds.contains(userId)) {
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
     * Saturday at 10:00 UTC — remind active users about the weekly challenge
     * if they haven't completed it yet. ~48 hours before week ends (Sunday midnight).
     */
    @Scheduled(cron = "0 0 10 * * SAT")
    public void sendWeeklyChallengeReminders() {
        try {
            ChallengeDefinition weekly = challengePoolService.getThisWeekChallenge();
            if (weekly == null) {
                log.info("No weekly challenge active, skipping weekly reminder");
                return;
            }

            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            String weekKey = String.format("WEEKLY-%d-W%02d", today.getYear(),
                    today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));

            List<Document> activeUsers = findActiveUsers();

            // Batch query: get all userIds who already completed this week's challenge
            List<String> allUserIds = activeUsers.stream()
                    .map(u -> u.getString("userId"))
                    .filter(id -> id != null)
                    .toList();
            Set<String> completedUserIds = new java.util.HashSet<>(
                    challengeLogRepository.findUserIdsWithChallengeDate(weekKey, allUserIds));

            int sent = 0;
            for (Document user : activeUsers) {
                String userId = user.getString("userId");
                String displayName = user.getString("displayName");
                if (userId == null) continue;

                if (completedUserIds.contains(userId)) {
                    continue;
                }

                ReminderEvent event = ReminderEvent.builder()
                        .userId(userId)
                        .displayName(displayName != null ? displayName : "Chef")
                        .reminderType("CHALLENGE_REMINDER")
                        .content(String.format(
                                "\uD83D\uDCC5 Weekly challenge ending soon: %s — 2 days left!",
                                weekly.getTitle()))
                        .priority(ReminderEvent.ReminderPriority.HIGH)
                        .hoursRemaining(48)
                        .challengeCategory(weekly.getId())
                        .build();

                kafkaTemplate.send(REMINDER_TOPIC, event);
                sent++;
            }
            log.info("WEEKLY_CHALLENGE_REMINDER sent to {} users", sent);
        } catch (Exception e) {
            log.error("Weekly challenge reminder scheduler failed", e);
        }
    }

    /**
     * Daily at 09:00 UTC — remind about seasonal challenges ending within 24 hours.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendSeasonalChallengeReminders() {
        try {
            Instant now = Instant.now();
            Instant twentyFourHoursFromNow = now.plus(24, ChronoUnit.HOURS);

            List<SeasonalChallenge> endingSoon = seasonalChallengeRepository
                    .findByStatusAndEndsAtAfter("ACTIVE", now)
                    .stream()
                    .filter(sc -> sc.getEndsAt().isBefore(twentyFourHoursFromNow))
                    .toList();

            if (endingSoon.isEmpty()) return;

            List<Document> activeUsers = findActiveUsers();

            for (SeasonalChallenge sc : endingSoon) {
                int sent = 0;
                for (Document user : activeUsers) {
                    String userId = user.getString("userId");
                    String displayName = user.getString("displayName");
                    if (userId == null) continue;

                    ReminderEvent event = ReminderEvent.builder()
                            .userId(userId)
                            .displayName(displayName != null ? displayName : "Chef")
                            .reminderType("CHALLENGE_REMINDER")
                            .content(String.format(
                                    "%s Seasonal challenge ending tomorrow: %s — Don't miss the %s badge!",
                                    sc.getEmoji() != null ? sc.getEmoji() : "\uD83C\uDF1F",
                                    sc.getTitle(),
                                    sc.getRewardBadgeName() != null ? sc.getRewardBadgeName() : "exclusive"))
                            .priority(ReminderEvent.ReminderPriority.HIGH)
                            .hoursRemaining(24)
                            .challengeCategory(sc.getId())
                            .build();

                    kafkaTemplate.send(REMINDER_TOPIC, event);
                    sent++;
                }
                log.info("SEASONAL_CHALLENGE_REMINDER for '{}' sent to {} users", sc.getTitle(), sent);
            }
        } catch (Exception e) {
            log.error("Seasonal challenge reminder scheduler failed", e);
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
