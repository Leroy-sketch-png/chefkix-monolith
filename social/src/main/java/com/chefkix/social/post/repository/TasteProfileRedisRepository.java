package com.chefkix.social.post.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TasteProfileRedisRepository {

    private static final String KEY_PREFIX = "feed:taste-profile:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final TypeReference<Map<String, Double>> MAP_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<Map<String, Double>> find(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        try {
            String payload = redisTemplate.opsForValue().get(key(userId));
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, MAP_TYPE));
        } catch (Exception e) {
            log.warn("Failed to read taste profile cache for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void save(String userId, Map<String, Double> tasteProfile) {
        if (userId == null || userId.isBlank() || tasteProfile == null) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(tasteProfile);
            redisTemplate.opsForValue().set(key(userId), payload, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize taste profile cache for user {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to write taste profile cache for user {}: {}", userId, e.getMessage());
        }
    }

    public void evict(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            redisTemplate.delete(key(userId));
        } catch (Exception e) {
            log.warn("Failed to evict taste profile cache for user {}: {}", userId, e.getMessage());
        }
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}