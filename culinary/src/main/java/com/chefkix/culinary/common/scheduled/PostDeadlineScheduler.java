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

import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 
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
     */
@Scheduled(cron = "0 0 9 * * *")
    public void checkPostDeadlines() {
        try {
            log.info("Running post deadline reminder check...");

            LocalDateTime now = utcNow();

            sendRemindersForDaysRemaining(now, 9, "NORMAL");

            sendRemindersForDaysRemaining(now, 2, "HIGH");

            sendRemindersForDaysRemaining(now, 1, "CRITICAL");
        } catch (Exception e) {
            log.error("Post deadline reminder scheduler failed — will retry next cycle", e);
        }
    }

    /**
     */
    @Scheduled(cron = "0 30 9 * * *")
    public void forfeitExpiredSessions() {
        try {
            log.info("Running expired session XP forfeiture check...");

            Query query = new Query();
            query.addCriteria(Criteria.where("status").is(SessionStatus.COMPLETED));
            query.addCriteria(Criteria.where("postId").isNull());
            query.addCriteria(Criteria.where("postDeadline").lt(utcNow()));
            query.addCriteria(Criteria.where("pendingXp").gt(0.0));

            Update update = new Update()
                .set("status", SessionStatus.EXPIRED)
                .set("pendingXp", 0.0);

            var result = mongoTemplate.updateMulti(query, update, CookingSession.class);

            if (result.getModifiedCount() > 0) {
                log.info("Forfeited XP for {} expired sessions", result.getModifiedCount());
            } else {
                log.info("No expired sessions found for XP forfeiture");
            }
        } catch (Exception e) {
            log.error("Expired session forfeiture failed — will retry next cycle", e);
        }
    }

    private void sendRemindersForDaysRemaining(LocalDateTime now, int daysRemaining, String priorityStr) {
        
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

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
