package com.chefkix.culinary.features.challenge.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed tracking for community challenge progress.
 * Uses atomic INCR for progress count and SADD for unique participant tracking.
 * Key patterns:
 *   community:challenge:{id}:progress    — integer counter
 *   community:challenge:{id}:participants — set of userId strings
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CommunityChallengeRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String PROGRESS_PREFIX = "community:challenge:";
    private static final String PROGRESS_SUFFIX = ":progress";
    private static final String PARTICIPANTS_SUFFIX = ":participants";

    /**
     * Atomically increment the community progress counter by 1.
     * @return the new progress value after increment
     */
    public long incrementProgress(String challengeId) {
        String key = PROGRESS_PREFIX + challengeId + PROGRESS_SUFFIX;
        try {
            Long val = redisTemplate.opsForValue().increment(key);
            return val != null ? val : 0;
        } catch (Exception e) {
            log.error("Failed to increment community challenge progress: {}", challengeId, e);
            return 0;
        }
    }

    /**
     * Get current progress for a community challenge.
     */
    public long getProgress(String challengeId) {
        String key = PROGRESS_PREFIX + challengeId + PROGRESS_SUFFIX;
        try {
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            log.error("Failed to get community challenge progress: {}", challengeId, e);
            return 0;
        }
    }

    /**
     * Add a user to the participants set. Returns true if the user was newly added.
     */
    public boolean addParticipant(String challengeId, String userId) {
        String key = PROGRESS_PREFIX + challengeId + PARTICIPANTS_SUFFIX;
        try {
            Long added = redisTemplate.opsForSet().add(key, userId);
            return added != null && added > 0;
        } catch (Exception e) {
            log.error("Failed to add participant to community challenge: {}", challengeId, e);
            return false;
        }
    }

    /**
     * Check if user is already a participant.
     */
    public boolean isParticipant(String challengeId, String userId) {
        String key = PROGRESS_PREFIX + challengeId + PARTICIPANTS_SUFFIX;
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Failed to check community challenge participation: {}", challengeId, e);
            return false;
        }
    }

    /**
     * Get total unique participant count.
     */
    public long getParticipantCount(String challengeId) {
        String key = PROGRESS_PREFIX + challengeId + PARTICIPANTS_SUFFIX;
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get community challenge participant count: {}", challengeId, e);
            return 0;
        }
    }

    /**
     * Set progress to a specific value (for seeding or corrections).
     */
    public void setProgress(String challengeId, long value) {
        String key = PROGRESS_PREFIX + challengeId + PROGRESS_SUFFIX;
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(value));
        } catch (Exception e) {
            log.error("Failed to set community challenge progress: {}", challengeId, e);
        }
    }
}
