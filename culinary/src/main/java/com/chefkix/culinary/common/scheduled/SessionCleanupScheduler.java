package com.chefkix.culinary.common.scheduled;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.session.entity.CookingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Cleans up stale cooking sessions that were never properly terminated.
 *
 * Two categories:
 * 1. PAUSED sessions whose resumeDeadline has passed → ABANDONED
 *    (User paused but never came back within the 3-hour window)
 * 2. COMPLETED sessions whose postDeadline has passed → EXPIRED
 *    (User finished cooking but never posted within 14 days — 70% pending XP forfeited)
 * 3. IN_PROGRESS sessions older than 12 hours → ABANDONED
 *    (Safety net — no real cooking session lasts 12 hours. User likely closed the app.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    private final MongoTemplate mongoTemplate;

    private static final int STALE_IN_PROGRESS_HOURS = 12;

    /**
     * Run every hour to catch stale sessions promptly.
     */
    @Scheduled(fixedRate = 3600000)  // Every 1 hour
    public void cleanupStaleSessions() {
        try {
            log.info("Running stale session cleanup...");

            LocalDateTime now = utcNow();

            long abandonedPaused = expirePausedSessions(now);
            long expiredCompleted = expireCompletedSessions(now);
            long abandonedInProgress = abandonStaleInProgressSessions(now);

            log.info("Session cleanup complete: {} paused→abandoned, {} completed→expired, {} in_progress→abandoned",
                    abandonedPaused, expiredCompleted, abandonedInProgress);
        } catch (Exception e) {
            log.error("Session cleanup scheduler failed — will retry next cycle", e);
        }
    }

    /**
     * PAUSED sessions past their resumeDeadline → ABANDONED.
     */
    private long expirePausedSessions(LocalDateTime now) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(SessionStatus.PAUSED));
        query.addCriteria(Criteria.where("resumeDeadline").lt(now));

        Update update = new Update()
                .set("status", SessionStatus.ABANDONED)
                .set("abandonedAt", now);

        var result = mongoTemplate.updateMulti(query, update, CookingSession.class);
        return result.getModifiedCount();
    }

    /**
     * COMPLETED sessions past their postDeadline with no postId → EXPIRED.
     * The 70% pending XP is forfeited (pendingXp stays on record but is never awarded).
     */
    private long expireCompletedSessions(LocalDateTime now) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(SessionStatus.COMPLETED));
        query.addCriteria(Criteria.where("postId").isNull());
        query.addCriteria(Criteria.where("postDeadline").lt(now));

        Update update = new Update()
                .set("status", SessionStatus.EXPIRED);

        var result = mongoTemplate.updateMulti(query, update, CookingSession.class);
        return result.getModifiedCount();
    }

    /**
     * IN_PROGRESS sessions older than 12 hours → ABANDONED.
     * Safety net for sessions where the user closed the app without pausing/completing.
     * Redis presence auto-expires after 4 hours, but the MongoDB doc stays IN_PROGRESS forever.
     */
    private long abandonStaleInProgressSessions(LocalDateTime now) {
        LocalDateTime cutoff = now.minusHours(STALE_IN_PROGRESS_HOURS);

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(SessionStatus.IN_PROGRESS));
        query.addCriteria(Criteria.where("startedAt").lt(cutoff));

        Update update = new Update()
                .set("status", SessionStatus.ABANDONED)
                .set("abandonedAt", now);

        var result = mongoTemplate.updateMulti(query, update, CookingSession.class);
        return result.getModifiedCount();
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
