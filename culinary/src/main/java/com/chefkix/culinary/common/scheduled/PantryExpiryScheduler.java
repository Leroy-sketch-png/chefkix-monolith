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

import java.time.LocalDate;
import java.util.List;

/**
 * Daily scheduler that alerts users about pantry items expiring within 3 days.
 * Sends "PANTRY_EXPIRING" reminders via Kafka → notification module.
 *
 * Runs daily at 08:00 UTC. Only alerts once per item per day (no spam).
 * Produces "use it or lose it" recipe suggestions in the notification content.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PantryExpiryScheduler {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REMINDER_TOPIC = "reminder-delivery";
    private static final String PANTRY_COLLECTION = "pantry_items";

    /**
     * Every day at 08:00 UTC — check for pantry items expiring within 3 days.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiringItems() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate threeDaysOut = today.plusDays(3);

            // Find distinct users with expiring items
            Query query = new Query(Criteria.where("expiryDate")
                    .gte(today).lte(threeDaysOut));
            List<Document> expiringItems = mongoTemplate.find(query, Document.class, PANTRY_COLLECTION);

            // Group by userId
            var byUser = new java.util.HashMap<String, List<String>>();
            for (Document item : expiringItems) {
                String userId = item.getString("userId");
                String ingredientName = item.getString("ingredientName");
                if (userId == null || ingredientName == null) continue;
                byUser.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(ingredientName);
            }

            log.info("Pantry expiry check: {} users have {} expiring items total",
                    byUser.size(), expiringItems.size());

            int sent = 0;
            for (var entry : byUser.entrySet()) {
                String userId = entry.getKey();
                List<String> ingredients = entry.getValue();

                String ingredientList = ingredients.size() <= 3
                        ? String.join(", ", ingredients)
                        : String.join(", ", ingredients.subList(0, 3)) + " + " + (ingredients.size() - 3) + " more";

                String message = String.format(
                        "Heads up! %s %s expiring soon. Cook something before %s go to waste!",
                        ingredientList,
                        ingredients.size() == 1 ? "is" : "are",
                        ingredients.size() == 1 ? "it" : "they");

                ReminderEvent event = ReminderEvent.builder()
                        .userId(userId)
                        .reminderType("PANTRY_EXPIRING")
                        .content(message)
                        .priority(ReminderEvent.ReminderPriority.NORMAL)
                        .daysRemaining(3)
                        .build();

                kafkaTemplate.send(REMINDER_TOPIC, event);
                sent++;
            }

            log.info("PANTRY_EXPIRING reminders sent to {} users", sent);
        } catch (Exception e) {
            log.error("Pantry expiry scheduler failed -- will retry tomorrow", e);
        }
    }
}
