package com.chefkix.identity.service;

import com.chefkix.identity.dto.response.PresenceResponse;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.repository.FollowRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Presence System — Redis heartbeat + WebSocket broadcast.
 *
 * Users send heartbeats via REST or STOMP. Redis stores presence with 90s TTL.
 * If no heartbeat within 90s, user is considered offline.
 *
 * Redis keys:
 *   presence:{userId}       → activity string ("browsing", "cooking:recipeTitle")
 *   presence:lastseen:{userId} → epoch millis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final String PREFIX = "presence:";
    private static final String LASTSEEN_PREFIX = "presence:lastseen:";
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(90);

    private final StringRedisTemplate redis;
    private final FollowRepository followRepository;
    private final UserProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Record a heartbeat for the user.
     *
     * @param userId   authenticated user
     * @param activity "browsing", "cooking", "creating", or "cooking:Recipe Title"
     */
    public void heartbeat(String userId, String activity) {
        String safeActivity = sanitizeActivity(activity);

        redis.opsForValue().set(PREFIX + userId, safeActivity, HEARTBEAT_TTL);
        redis.opsForValue().set(LASTSEEN_PREFIX + userId,
                String.valueOf(Instant.now().toEpochMilli()));

        // Broadcast to followers subscribed to /topic/presence
        messagingTemplate.convertAndSend("/topic/presence/" + userId,
                new PresenceEvent(userId, safeActivity, true));
    }

    /**
     * Mark user as offline (called on disconnect/logout).
     */
    public void goOffline(String userId) {
        redis.delete(PREFIX + userId);
        redis.opsForValue().set(LASTSEEN_PREFIX + userId,
                String.valueOf(Instant.now().toEpochMilli()));

        messagingTemplate.convertAndSend("/topic/presence/" + userId,
                new PresenceEvent(userId, "offline", false));
    }

    /**
     * Check if a specific user is online.
     */
    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + userId));
    }

    /**
     * Get presence of all followed users ("Friends Cooking Now").
     */
    public List<PresenceResponse> getFriendsPresence(String userId) {
        // Get who this user follows
        Set<String> followingIds = followRepository.findAllByFollowerId(userId).stream()
                .map(f -> f.getFollowingId())
                .collect(Collectors.toSet());

        if (followingIds.isEmpty()) return List.of();

        // Check Redis for online friends
        return followingIds.stream()
                .map(friendId -> {
                    String activity = redis.opsForValue().get(PREFIX + friendId);
                    if (activity == null) return null; // offline

                    String lastSeenStr = redis.opsForValue().get(LASTSEEN_PREFIX + friendId);
                    long lastSeen = lastSeenStr != null ? Long.parseLong(lastSeenStr) : 0;

                    // Parse activity
                    String activityType = activity;
                    String recipeTitle = null;
                    if (activity.startsWith("cooking:")) {
                        activityType = "cooking";
                        recipeTitle = activity.substring("cooking:".length());
                    }

                    // Get profile info
                    UserProfile profile = profileRepository.findByUserId(friendId).orElse(null);
                    if (profile == null) return null;

                    return PresenceResponse.builder()
                            .userId(friendId)
                            .username(profile.getUsername())
                            .displayName(profile.getDisplayName() != null ?
                                    profile.getDisplayName() : profile.getFirstName())
                            .avatarUrl(profile.getAvatarUrl())
                            .online(true)
                            .activity(activityType)
                            .recipeTitle(recipeTitle)
                            .lastSeenEpoch(lastSeen)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Get online friends who are currently cooking.
     */
    public List<PresenceResponse> getFriendsCookingNow(String userId) {
        return getFriendsPresence(userId).stream()
                .filter(p -> "cooking".equals(p.getActivity()))
                .toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String sanitizeActivity(String activity) {
        if (activity == null || activity.isBlank()) return "browsing";
        // Allow: browsing, cooking, cooking:RecipeTitle, creating, idle
        String trimmed = activity.trim();
        if (trimmed.length() > 200) trimmed = trimmed.substring(0, 200);
        // Strip any control characters
        return trimmed.replaceAll("[\\p{Cntrl}]", "");
    }

    // ── WebSocket event DTO ─────────────────────────────────────────

    public record PresenceEvent(String userId, String activity, boolean online) {}
}
