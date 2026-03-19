package com.chefkix.culinary.features.room.repository;

import com.chefkix.culinary.features.room.model.CookingRoom;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis-backed repository for ephemeral cooking rooms.
 * Rooms are stored as JSON strings with a 4-hour TTL.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CookingRoomRedisRepository {

    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "cooking-room:";
    private static final long TTL_SECONDS = 4 * 60 * 60; // 4 hours

    private String key(String roomCode) {
        return KEY_PREFIX + roomCode.toUpperCase();
    }

    public void save(CookingRoom room) {
        try {
            String json = objectMapper.writeValueAsString(room);
            redisTemplate.opsForValue().set(key(room.getRoomCode()), json, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize CookingRoom {}: {}", room.getRoomCode(), e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to save cooking room: " + e.getMessage());
        }
    }

    public Optional<CookingRoom> findByRoomCode(String roomCode) {
        String json = redisTemplate.opsForValue().get(key(roomCode.toUpperCase()));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CookingRoom.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize CookingRoom {}: {}", roomCode, e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String roomCode) {
        redisTemplate.delete(key(roomCode.toUpperCase()));
    }

    public boolean exists(String roomCode) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(roomCode.toUpperCase())));
    }

    /**
     * Refresh TTL on activity to prevent expiration during active cooking.
     */
    public void refreshTtl(String roomCode) {
        redisTemplate.expire(key(roomCode.toUpperCase()), TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Retrieve all active cooking rooms from Redis.
     * Scans for all keys with the room prefix and deserializes them.
     */
    public List<CookingRoom> findAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return List.of();

        return keys.stream()
                .map(k -> redisTemplate.opsForValue().get(k))
                .filter(json -> json != null)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, CookingRoom.class);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize room: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(room -> room != null)
                .collect(Collectors.toList());
    }
}
