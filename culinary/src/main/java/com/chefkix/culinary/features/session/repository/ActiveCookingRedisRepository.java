package com.chefkix.culinary.features.session.repository;

import com.chefkix.culinary.features.session.model.ActiveCookingPresence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis-backed repository for tracking active cooking sessions.
 * Enables the "Friends Cooking Now" feature with O(1) lookups per friend
 * via Redis MGET instead of MongoDB queries.
 *
 * Key pattern: cooking:active:{userId} → JSON(ActiveCookingPresence)
 * TTL: 4 hours (safety net — keys are explicitly deleted on session end)
 */
@Repository
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActiveCookingRedisRepository {

    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "cooking:active:";
    private static final long TTL_SECONDS = 4 * 60 * 60; // 4 hours

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * Mark a user as actively cooking. Called on session start and resume.
     */
    public void setActive(ActiveCookingPresence presence) {
        try {
            String json = objectMapper.writeValueAsString(presence);
            redisTemplate.opsForValue().set(key(presence.getUserId()), json, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ActiveCookingPresence for user {}: {}", presence.getUserId(), e.getMessage());
        }
    }

    /**
     * Remove a user's active cooking status. Called on session complete, abandon, or pause.
     */
    public void removeActive(String userId) {
        redisTemplate.delete(key(userId));
    }

    /**
     * Get a single user's active cooking presence.
     */
    public Optional<ActiveCookingPresence> getActive(String userId) {
        String json = redisTemplate.opsForValue().get(key(userId));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ActiveCookingPresence.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ActiveCookingPresence for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Batch-fetch active cooking presences for multiple user IDs.
     * Uses Redis MGET for efficiency — single round-trip regardless of friend count.
     */
    public List<ActiveCookingPresence> getActiveForUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();

        List<String> keys = userIds.stream().map(this::key).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        if (values == null) return List.of();

        return values.stream()
                .filter(Objects::nonNull)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ActiveCookingPresence.class);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize cooking presence: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
