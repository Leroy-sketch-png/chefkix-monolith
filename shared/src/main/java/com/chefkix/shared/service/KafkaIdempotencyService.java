package com.chefkix.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "kafka:processed:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     *
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
     */
    public void removeProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) return;
        String key = KEY_PREFIX + eventId;
        redisTemplate.delete(key);
        log.debug("Removed idempotency mark for event {} to allow retry", eventId);
    }

    /**
     *
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

    /**
     *
     */
    public void removeProcessed(String eventId, String topic) {
        if (eventId == null || eventId.isBlank()) return;
        String key = KEY_PREFIX + topic + ":" + eventId;
        redisTemplate.delete(key);
        log.debug("Removed idempotency mark for event {} on topic {} to allow retry", eventId, topic);
    }
}
