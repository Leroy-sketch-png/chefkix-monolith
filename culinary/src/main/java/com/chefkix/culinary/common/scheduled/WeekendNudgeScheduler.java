package com.chefkix.culinary.common.scheduled;

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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Saturday morning nudge for users who haven't cooked in 3+ days.
 * Sends a friendly "weekend cooking inspiration" reminder via Kafka.
 * <p>
 * Runs every Saturday at 10:00 UTC.
 * Targets users who have cooked before but haven't in the last 3 days,
 * encouraging them to get back in the kitchen on the weekend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeekendNudgeScheduler {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REMINDER_TOPIC = "reminder-delivery";
    private static final String USER_PROFILES_COLLECTION = "user_profiles";
    private static final int INACTIVE_DAYS = 3;

    private static final String[] NUDGE_MESSAGES = {
            "It's the weekend! Time to try something new in the kitchen.",
            "Weekend vibes call for weekend recipes. What are you cooking today?",
            "Your kitchen misses you! Pick a recipe and earn some XP this weekend.",
            "Saturday = cooking day. Check out today's trending recipes!",
            "The weekend is here. Your streak could use some love!"
    };

    /**
     * Every Saturday at 10:00 UTC — nudge inactive users to cook on the weekend.
     */
    @Scheduled(cron = "0 0 10 * * SAT")
    public void sendWeekendNudges() {
        try {
            List<Document> inactiveUsers = findInactiveUsersWhoPreviouslyCooked();
            log.info("Sending WEEKEND_NUDGE to {} users who haven't cooked in {}+ days",
                    inactiveUsers.size(), INACTIVE_DAYS);

            int sent = 0;
            for (Document user : inactiveUsers) {
                String userId = user.getString("userId");
                String displayName = user.getString("displayName");
                if (userId == null) continue;

                String message = NUDGE_MESSAGES[sent % NUDGE_MESSAGES.length];

                ReminderEvent event = ReminderEvent.builder()
                        .userId(userId)
                        .displayName(displayName != null ? displayName : "Chef")
                        .reminderType("WEEKEND_NUDGE")
                        .content(message)
                        .priority(ReminderEvent.ReminderPriority.NORMAL)
                        .build();

                kafkaTemplate.send(REMINDER_TOPIC, event);
                sent++;
            }

            log.info("WEEKEND_NUDGE sent to {} users", sent);
        } catch (Exception e) {
            log.error("Weekend nudge scheduler failed -- will retry next week", e);
        }
    }

    /**
     * Finds users who have cooked before (lastCookAt exists) but not in the last N days.
     * This avoids nudging brand-new users who never cooked, and avoids nudging
     * users who are already active.
     */
    private List<Document> findInactiveUsersWhoPreviouslyCooked() {
        Instant inactiveThreshold = Instant.now().minus(INACTIVE_DAYS, ChronoUnit.DAYS);

        Query query = new Query();
        query.addCriteria(Criteria.where("statistics.lastCookAt")
                .exists(true)
                .lt(inactiveThreshold));
        query.fields().include("userId").include("displayName");

        return mongoTemplate.find(query, Document.class, USER_PROFILES_COLLECTION);
    }
}
