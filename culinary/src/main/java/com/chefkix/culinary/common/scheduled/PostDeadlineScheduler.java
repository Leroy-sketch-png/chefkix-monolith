package com.chefkix.culinary.common.scheduled;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.shared.event.ReminderEvent;
import com.chefkix.culinary.features.session.entity.CookingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to send post deadline reminder notifications.
 * 
 * Per spec (04-statistics.txt):
 * - Post deadline is 14 days from session completion
 * - Day 5 reminder (9 days left): Normal priority
 * - Day 12 reminder (2 days left): High priority
 * - After 14 days: 70% pending XP forfeited
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeadlineScheduler {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProfileProvider profileProvider;

    private static final String REMINDER_TOPIC = "reminder-delivery";

    /**
     * Run every day at 9 AM to check for sessions with approaching deadlines.
     */
    @Scheduled(cron = "0 0 9 * * *")  // Every day at 9:00 AM
    public void checkPostDeadlines() {
        log.info("Running post deadline reminder check...");

        LocalDateTime now = LocalDateTime.now();

        // Day 5 reminder (9 days left until 14-day deadline)
        sendRemindersForDaysRemaining(now, 9, "NORMAL");

        // Day 12 reminder (2 days left until 14-day deadline)
        sendRemindersForDaysRemaining(now, 2, "HIGH");

        // Final reminder (1 day left - last chance)
        sendRemindersForDaysRemaining(now, 1, "CRITICAL");
    }

    private void sendRemindersForDaysRemaining(LocalDateTime now, int daysRemaining, String priorityStr) {
        // Find sessions where:
        // 1. Status = COMPLETED
        // 2. postId = null (not yet posted)
        // 3. postDeadline is daysRemaining days from now (±12 hours window)
        
        LocalDateTime deadlineWindowStart = now.plusDays(daysRemaining).minusHours(12);
        LocalDateTime deadlineWindowEnd = now.plusDays(daysRemaining).plusHours(12);

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(SessionStatus.COMPLETED));
        query.addCriteria(Criteria.where("postId").isNull());
        query.addCriteria(Criteria.where("postDeadline").gte(deadlineWindowStart).lte(deadlineWindowEnd));

        List<CookingSession> sessions = mongoTemplate.find(query, CookingSession.class);

        log.info("Found {} sessions with {} days until deadline", sessions.size(), daysRemaining);

        for (CookingSession session : sessions) {
            sendDeadlineReminder(session, daysRemaining, priorityStr);
        }
    }

    private void sendDeadlineReminder(CookingSession session, int daysRemaining, String priorityStr) {
        String displayName = "Chef";
        
        // Try to get user's display name
        try {
            BasicProfileInfo profile = profileProvider.getBasicProfile(session.getUserId());
            if (profile != null && profile.getDisplayName() != null) {
                displayName = profile.getDisplayName();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch profile for user {}: {}", session.getUserId(), e.getMessage());
        }

        String content;
        if (daysRemaining == 1) {
            content = String.format("⚠️ Last chance! Post your \"%s\" attempt today to claim your XP!", 
                session.getRecipeTitle());
        } else if (daysRemaining <= 2) {
            content = String.format("🔥 Only %d days left to post your \"%s\" attempt for full XP!", 
                daysRemaining, session.getRecipeTitle());
        } else {
            content = String.format("📸 Share your \"%s\" cooking! Post in %d days for full XP.", 
                session.getRecipeTitle(), daysRemaining);
        }

        ReminderEvent event = ReminderEvent.builder()
            .userId(session.getUserId())
            .displayName(displayName)
            .reminderType("POST_DEADLINE")
            .content(content)
            .priority(ReminderEvent.ReminderPriority.valueOf(priorityStr))
            .sessionId(session.getId())
            .recipeTitle(session.getRecipeTitle())
            .daysRemaining(daysRemaining)
            .build();

        kafkaTemplate.send(REMINDER_TOPIC, event);

        log.info("Sent post deadline reminder to user {} for session {} ({} days left)", 
            session.getUserId(), session.getId(), daysRemaining);
    }
}
