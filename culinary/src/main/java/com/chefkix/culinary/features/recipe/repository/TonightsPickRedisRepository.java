package com.chefkix.culinary.features.recipe.repository;

import com.chefkix.culinary.features.recipe.dto.response.RecommendationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TonightsPickRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<RecommendationResponse> find(String key) {
        try {
            String payload = redisTemplate.opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, RecommendationResponse.class));
        } catch (Exception e) {
            log.warn("Failed to read tonight's pick cache for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void save(String key, RecommendationResponse response, Duration ttl) {
        if (response == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, payload, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize tonight's pick cache for key {}: {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to write tonight's pick cache for key {}: {}", key, e.getMessage());
        }
    }
}