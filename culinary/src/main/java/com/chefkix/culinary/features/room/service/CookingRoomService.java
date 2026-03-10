package com.chefkix.culinary.features.room.service;

import com.chefkix.culinary.features.room.dto.request.CreateRoomRequest;
import com.chefkix.culinary.features.room.dto.request.InviteToRoomRequest;
import com.chefkix.culinary.features.room.dto.request.JoinRoomRequest;
import com.chefkix.culinary.features.room.dto.response.CookingRoomResponse;
import com.chefkix.culinary.features.room.dto.response.FriendsActiveRoomResponse;
import com.chefkix.culinary.features.room.dto.response.LeaveRoomResponse;
import com.chefkix.culinary.features.room.model.CookingRoom;
import com.chefkix.culinary.features.room.model.RoomEvent;
import com.chefkix.culinary.features.room.model.RoomEventType;
import com.chefkix.culinary.features.room.model.RoomParticipant;
import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import com.chefkix.culinary.features.session.dto.request.StartSessionRequest;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.ReminderEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.Comparator;

/**
 * Manages ephemeral co-cooking rooms backed by Redis.
 * Rooms are an awareness overlay on top of individual CookingSessions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CookingRoomService {

    CookingRoomRedisRepository roomRepository;
    CookingSessionService sessionService;
    CookingSessionRepository sessionRepository;
    ProfileProvider profileProvider;
    SimpMessagingTemplate messagingTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REMINDER_TOPIC = "reminder-delivery";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No I/O/0/1 to avoid confusion
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ────────────────────────────────────────────────────────
    // CREATE ROOM
    // ────────────────────────────────────────────────────────

    public CookingRoomResponse createRoom(String userId, CreateRoomRequest request) {
        // Generate unique room code
        String roomCode = generateUniqueRoomCode();

        // Get or create a cooking session for this recipe (linked to room for co-op XP)
        String sessionId = getOrCreateSession(userId, request.getRecipeId(), roomCode);

        // Get user profile for participant info
        BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
        String displayName = resolveDisplayName(profile);

        RoomParticipant host = RoomParticipant.builder()
                .userId(userId)
                .displayName(displayName)
                .avatarUrl(profile.getAvatarUrl())
                .sessionId(sessionId)
                .currentStep(1)
                .completedSteps(new ArrayList<>())
                .joinedAt(Instant.now())
                .isHost(true)
                .build();

        // Fetch recipe title from session (the session already validated the recipe)
        CookingSession session = sessionRepository.findById(sessionId).orElse(null);
        String recipeTitle = session != null ? session.getRecipeTitle() : "";

        CookingRoom room = CookingRoom.builder()
                .roomCode(roomCode)
                .recipeId(request.getRecipeId())
                .recipeTitle(recipeTitle)
                .hostUserId(userId)
                .status(CookingRoom.STATUS_WAITING)
                .maxParticipants(6)
                .createdAt(Instant.now())
                .participants(new ArrayList<>(List.of(host)))
                .build();

        roomRepository.save(room);
        log.info("Room {} created by {} for recipe {}", roomCode, userId, request.getRecipeId());

        return toResponse(room, sessionId);
    }

    // ────────────────────────────────────────────────────────
    // JOIN ROOM
    // ────────────────────────────────────────────────────────

    public CookingRoomResponse joinRoom(String userId, JoinRoomRequest request) {
        String roomCode = request.getRoomCode().toUpperCase();
        String role = request.getRole() != null ? request.getRole().toUpperCase() : "COOK";
        boolean isSpectator = "SPECTATOR".equals(role);

        CookingRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (CookingRoom.STATUS_DISSOLVED.equals(room.getStatus())) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }

        // Check if already in room
        boolean alreadyIn = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (alreadyIn) {
            throw new AppException(ErrorCode.ALREADY_IN_ROOM);
        }

        // Check capacity
        if (room.getParticipants().size() >= room.getMaxParticipants()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }

        // Spectators don't get a cooking session — they're just watching
        String sessionId = null;
        if (!isSpectator) {
            sessionId = getOrCreateSession(userId, room.getRecipeId(), roomCode);
        }

        // Build participant
        BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
        String displayName = resolveDisplayName(profile);

        RoomParticipant participant = RoomParticipant.builder()
                .userId(userId)
                .displayName(displayName)
                .avatarUrl(profile.getAvatarUrl())
                .sessionId(sessionId)
                .currentStep(1)
                .completedSteps(new ArrayList<>())
                .joinedAt(Instant.now())
                .isHost(false)
                .role(role)
                .build();

        room.getParticipants().add(participant);

        // Transition to COOKING when 2+ COOK participants
        long cookCount = room.getParticipants().stream()
                .filter(p -> !"SPECTATOR".equals(p.getRole()))
                .count();
        if (cookCount >= 2 && CookingRoom.STATUS_WAITING.equals(room.getStatus())) {
            room.setStatus(CookingRoom.STATUS_COOKING);
        }

        roomRepository.save(room);
        log.info("User {} joined room {} as {}", userId, roomCode, role);

        // Broadcast join event
        broadcastEvent(roomCode, RoomEventType.PARTICIPANT_JOINED, userId, displayName,
                Map.of("userId", userId, "displayName", displayName,
                        "avatarUrl", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : "",
                        "role", role));

        return toResponse(room, sessionId);
    }

    // ────────────────────────────────────────────────────────
    // LEAVE ROOM
    // ────────────────────────────────────────────────────────

    public LeaveRoomResponse leaveRoom(String userId, String roomCode) {
        roomCode = roomCode.toUpperCase();

        CookingRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // Remove participant
        boolean removed = room.getParticipants().removeIf(p -> p.getUserId().equals(userId));
        if (!removed) {
            throw new AppException(ErrorCode.NOT_IN_ROOM);
        }

        String newHostUserId = null;
        boolean dissolved = false;

        if (room.getParticipants().isEmpty()) {
            // Last person left — dissolve room
            room.setStatus(CookingRoom.STATUS_DISSOLVED);
            roomRepository.delete(roomCode);
            dissolved = true;
            log.info("Room {} dissolved (last participant left)", roomCode);

            broadcastEvent(roomCode, RoomEventType.ROOM_DISSOLVED, userId, null, Map.of());
        } else {
            // If host left, transfer to longest-active participant
            if (userId.equals(room.getHostUserId())) {
                RoomParticipant newHost = room.getParticipants().stream()
                        .min(Comparator.comparing(RoomParticipant::getJoinedAt))
                        .orElseThrow();

                newHost.setHost(true);
                room.setHostUserId(newHost.getUserId());
                newHostUserId = newHost.getUserId();

                log.info("Room {} host transferred from {} to {}", roomCode, userId, newHostUserId);

                broadcastEvent(roomCode, RoomEventType.HOST_TRANSFERRED, userId, null,
                        Map.of("newHostUserId", newHostUserId, "reason", "host_left"));
            }

            roomRepository.save(room);

            // Broadcast leave
            broadcastEvent(roomCode, RoomEventType.PARTICIPANT_LEFT, userId, null,
                    Map.of("userId", userId,
                            "newHostUserId", newHostUserId != null ? newHostUserId : ""));
        }

        return LeaveRoomResponse.builder()
                .left(true)
                .roomDissolved(dissolved)
                .newHostUserId(newHostUserId)
                .build();
    }

    // ────────────────────────────────────────────────────────
    // GET ROOM
    // ────────────────────────────────────────────────────────

    public CookingRoomResponse getRoom(String userId, String roomCode) {
        roomCode = roomCode.toUpperCase();

        CookingRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // Find this user's session ID
        String sessionId = room.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .map(RoomParticipant::getSessionId)
                .orElse(null);

        roomRepository.refreshTtl(roomCode);
        return toResponse(room, sessionId);
    }

    // ────────────────────────────────────────────────────────
    // UPDATE PARTICIPANT STATE (called from WebSocket handlers)
    // ────────────────────────────────────────────────────────

    public void updateParticipantStep(String roomCode, String userId, int stepNumber) {
        roomRepository.findByRoomCode(roomCode).ifPresent(room -> {
            room.getParticipants().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> p.setCurrentStep(stepNumber));
            roomRepository.save(room);
            roomRepository.refreshTtl(roomCode);
        });
    }

    public void updateParticipantCompletedSteps(String roomCode, String userId,
                                                  int stepNumber, List<Integer> completedSteps) {
        roomRepository.findByRoomCode(roomCode).ifPresent(room -> {
            room.getParticipants().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> {
                        if (completedSteps != null) {
                            p.setCompletedSteps(completedSteps);
                        } else {
                            // Add single step if completedSteps list not provided
                            if (!p.getCompletedSteps().contains(stepNumber)) {
                                p.getCompletedSteps().add(stepNumber);
                            }
                        }
                    });
            roomRepository.save(room);
            roomRepository.refreshTtl(roomCode);
        });
    }

    // ────────────────────────────────────────────────────────
    // INVITE TO ROOM
    // ────────────────────────────────────────────────────────

    /**
     * Sends a room invite notification to a target user.
     * Validates sender is in room, target is not already in room.
     * Notification deep-links to /cook-together?roomCode=XXX.
     */
    public void inviteToRoom(String senderId, String roomCode, InviteToRoomRequest request) {
        roomCode = roomCode.toUpperCase();

        CookingRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // Sender must be in the room
        boolean senderInRoom = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(senderId));
        if (!senderInRoom) {
            throw new AppException(ErrorCode.NOT_IN_ROOM);
        }

        // Target must not already be in room
        boolean targetInRoom = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(request.getUserId()));
        if (targetInRoom) {
            throw new AppException(ErrorCode.ALREADY_IN_ROOM);
        }

        // Room must not be full
        if (room.getParticipants().size() >= room.getMaxParticipants()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }

        BasicProfileInfo senderProfile = profileProvider.getBasicProfile(senderId);
        String senderName = resolveDisplayName(senderProfile);

        ReminderEvent event = ReminderEvent.builder()
                .userId(request.getUserId())
                .displayName(senderName)
                .reminderType("ROOM_INVITE")
                .content(String.format("🍳 %s invited you to cook %s together!", senderName, room.getRecipeTitle()))
                .priority(ReminderEvent.ReminderPriority.HIGH)
                .recipeTitle(room.getRecipeTitle())
                .roomCode(roomCode)
                .build();

        kafkaTemplate.send(REMINDER_TOPIC, event);
        log.info("Room invite sent: {} → {} for room {}", senderId, request.getUserId(), roomCode);
    }

    // ────────────────────────────────────────────────────────
    // FRIENDS ACTIVE ROOMS
    // ────────────────────────────────────────────────────────

    /**
     * Returns active rooms where users the caller follows are currently cooking.
     * Used for "Friends Cooking Now" widget — poll every 30s.
     */
    public List<FriendsActiveRoomResponse> getFriendsActiveRooms(String userId) {
        List<String> followingIds = profileProvider.getFollowingIds(userId);
        if (followingIds.isEmpty()) return List.of();

        Set<String> followingSet = new HashSet<>(followingIds);

        return roomRepository.findAll().stream()
                .filter(room -> !CookingRoom.STATUS_DISSOLVED.equals(room.getStatus()))
                .filter(room -> room.getParticipants().stream()
                        .anyMatch(p -> followingSet.contains(p.getUserId())))
                .map(room -> {
                    List<String> friendNames = room.getParticipants().stream()
                            .filter(p -> followingSet.contains(p.getUserId()))
                            .map(RoomParticipant::getDisplayName)
                            .toList();

                    long minutesAgo = java.time.Duration.between(room.getCreatedAt(), Instant.now()).toMinutes();

                    return FriendsActiveRoomResponse.builder()
                            .roomCode(room.getRoomCode())
                            .recipeId(room.getRecipeId())
                            .recipeTitle(room.getRecipeTitle())
                            .participantCount(room.getParticipants().size())
                            .participantNames(room.getParticipants().stream()
                                    .map(RoomParticipant::getDisplayName).toList())
                            .startedMinutesAgo(minutesAgo)
                            .build();
                })
                .toList();
    }

    // ────────────────────────────────────────────────────────
    // CO-OP XP MULTIPLIER
    // ────────────────────────────────────────────────────────

    /**
     * Calculates the co-op XP multiplier for a room-linked session.
     * 2 cooks = 1.2×, 3+ cooks = 1.1×, solo/spectator-only = 1.0×.
     */
    public double getCoOpMultiplier(String roomCode) {
        if (roomCode == null) return 1.0;

        return roomRepository.findByRoomCode(roomCode)
                .map(room -> {
                    long cookCount = room.getParticipants().stream()
                            .filter(p -> !"SPECTATOR".equals(p.getRole()))
                            .count();
                    if (cookCount == 2) return 1.2;
                    if (cookCount >= 3) return 1.1;
                    return 1.0;
                })
                .orElse(1.0);
    }

    // ────────────────────────────────────────────────────────
    // BROADCAST HELPER
    // ────────────────────────────────────────────────────────

    public void broadcastEvent(String roomCode, RoomEventType type,
                                String userId, String displayName,
                                Map<String, Object> data) {
        RoomEvent event = RoomEvent.builder()
                .type(type)
                .userId(userId)
                .displayName(displayName)
                .timestamp(Instant.now())
                .data(data)
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), event);
    }

    // ────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ────────────────────────────────────────────────────────

    /**
     * Returns true if the given user is a spectator in the given room.
     * Used by WsController to guard cooking-specific events.
     */
    public boolean isSpectator(String roomCode, String userId) {
        return roomRepository.findByRoomCode(roomCode.toUpperCase())
                .map(room -> room.getParticipants().stream()
                        .filter(p -> p.getUserId().equals(userId))
                        .findFirst()
                        .map(p -> "SPECTATOR".equals(p.getRole()))
                        .orElse(false))
                .orElse(false);
    }

    /**
     * Gets existing IN_PROGRESS session for recipe, or starts a new one.
     * Links the session to the room so co-op XP multiplier applies on completion.
     */
    private String getOrCreateSession(String userId, String recipeId, String roomCode) {
        // Check if user already has an active session
        Optional<CookingSession> existing = sessionRepository
                .findFirstByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS);

        if (existing.isPresent()) {
            CookingSession session = existing.get();
            if (session.getRecipeId().equals(recipeId)) {
                // Link existing session to room if not already linked
                if (session.getRoomCode() == null && roomCode != null) {
                    session.setRoomCode(roomCode);
                    sessionRepository.save(session);
                }
                return session.getId();
            }
            // Different recipe — can't join room while cooking something else
            throw new AppException(ErrorCode.SESSION_ALREADY_ACTIVE);
        }

        // Start a new session
        var response = sessionService.startSession(userId,
                StartSessionRequest.builder().recipeId(recipeId).build());

        // Link the newly created session to the room
        if (roomCode != null) {
            sessionRepository.findById(response.getSessionId()).ifPresent(session -> {
                session.setRoomCode(roomCode);
                sessionRepository.save(session);
            });
        }

        return response.getSessionId();
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                code.append(ROOM_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(ROOM_CODE_CHARS.length())));
            }
            String roomCode = code.toString();
            if (!roomRepository.exists(roomCode)) {
                return roomCode;
            }
        }
        throw new RuntimeException("Failed to generate unique room code after 10 attempts");
    }

    private String resolveDisplayName(BasicProfileInfo profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        if (profile.getFirstName() != null) {
            String name = profile.getFirstName();
            if (profile.getLastName() != null) name += " " + profile.getLastName();
            return name;
        }
        return profile.getUsername() != null ? profile.getUsername() : "Unknown";
    }

    private CookingRoomResponse toResponse(CookingRoom room, String sessionId) {
        return CookingRoomResponse.builder()
                .roomCode(room.getRoomCode())
                .recipeId(room.getRecipeId())
                .recipeTitle(room.getRecipeTitle())
                .hostUserId(room.getHostUserId())
                .status(room.getStatus())
                .maxParticipants(room.getMaxParticipants())
                .participants(room.getParticipants())
                .createdAt(room.getCreatedAt())
                .sessionId(sessionId)
                .build();
    }
}
