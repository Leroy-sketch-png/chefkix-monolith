package com.chefkix.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based idempotency service for Kafka event processing.
 * <p>
 * Prevents duplicate processing when Kafka redelivers messages due to:
 * - Consumer restarts
 * - Partition rebalancing
 * - Network issues
 * <p>
 * Events are deduplicated using their unique eventId stored in Redis with TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "kafka:processed:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * Attempts to mark an event as processed.
     * Returns true if the event was NOT previously processed (safe to process).
     * Returns false if the event was already processed (skip it).
     *
     * @param eventId unique event identifier
     * @return true if event should be processed, false if duplicate
     */
    public boolean tryProcess(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.error("Received event with null/blank eventId - skipping to protect idempotency guarantees");
            return false;
        }

        String key = KEY_PREFIX + eventId;
        Boolean isNewEvent = redisTemplate.opsForValue().setIfAbsent(key, "1", DEFAULT_TTL);

        if (Boolean.TRUE.equals(isNewEvent)) {
            log.debug("Event {} marked as processing", eventId);
            return true;
        } else {
            log.info("Duplicate event {} detected - skipping", eventId);
            return false;
        }
    }

    /**
     * Attempts to mark an event as processed with a specific topic for debugging.
     *
     * @param eventId unique event identifier
     * @param topic   Kafka topic for logging
     * @return true if event should be processed, false if duplicate
     */
    public boolean tryProcess(String eventId, String topic) {
        if (eventId == null || eventId.isBlank()) {
            log.error("Received event with null/blank eventId on topic {} - skipping", topic);
            return false;
        }

        String key = KEY_PREFIX + topic + ":" + eventId;
        Boolean isNewEvent = redisTemplate.opsForValue().setIfAbsent(key, "1", DEFAULT_TTL);

        if (Boolean.TRUE.equals(isNewEvent)) {
            log.debug("Event {} on topic {} marked as processing", eventId, topic);
            return true;
        } else {
            log.info("Duplicate event {} on topic {} detected - skipping", eventId, topic);
            return false;
        }
    }
}
