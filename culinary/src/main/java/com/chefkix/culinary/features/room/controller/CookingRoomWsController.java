package com.chefkix.culinary.features.room.controller;

import com.chefkix.culinary.features.room.dto.request.RoomEventRequest;
import com.chefkix.culinary.features.room.model.RoomEventType;
import com.chefkix.culinary.features.room.service.CookingRoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for real-time co-cooking events.
 * Clients publish to /app/room.* destinations; events are broadcast
 * to /topic/room/{roomCode} for all room participants.
 *
 * Pattern follows ChatWebSocketController: @MessageMapping → service → broadcast.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CookingRoomWsController {

    CookingRoomService roomService;

    @MessageMapping("/room.stepNavigated")
    public void handleStepNavigated(RoomEventRequest request) {
        String userId = getUserId();
        if (request.getRoomCode() == null || request.getStepNumber() == null) return;
        if (!roomService.isParticipant(request.getRoomCode(), userId)) return;
        if (roomService.isSpectator(request.getRoomCode(), userId)) return; // Spectators can't navigate

        // Update participant state in Redis
        roomService.updateParticipantStep(request.getRoomCode(), userId, request.getStepNumber());

        // Broadcast to room
        roomService.broadcastEvent(request.getRoomCode(), RoomEventType.STEP_NAVIGATED,
                userId, null,
                Map.of("stepNumber", request.getStepNumber()));
    }

    @MessageMapping("/room.stepCompleted")
    public void handleStepCompleted(RoomEventRequest request) {
        String userId = getUserId();
        if (request.getRoomCode() == null || request.getStepNumber() == null) return;
        if (!roomService.isParticipant(request.getRoomCode(), userId)) return;
        if (roomService.isSpectator(request.getRoomCode(), userId)) return; // Spectators can't complete steps

        // Update completed steps in Redis
        roomService.updateParticipantCompletedSteps(
                request.getRoomCode(), userId,
                request.getStepNumber(), request.getCompletedSteps());

        Map<String, Object> data = new HashMap<>();
        data.put("stepNumber", request.getStepNumber());
        if (request.getCompletedSteps() != null) {
            data.put("completedSteps", request.getCompletedSteps());
        }

        roomService.broadcastEvent(request.getRoomCode(), RoomEventType.STEP_COMPLETED,
                userId, null, data);
    }

    @MessageMapping("/room.timerStarted")
    public void handleTimerStarted(RoomEventRequest request) {
        String userId = getUserId();
        if (request.getRoomCode() == null) return;
        if (!roomService.isParticipant(request.getRoomCode(), userId)) return;
        if (roomService.isSpectator(request.getRoomCode(), userId)) return; // Spectators can't start timers

        Map<String, Object> data = new HashMap<>();
        if (request.getStepNumber() != null) data.put("stepNumber", request.getStepNumber());
        if (request.getTotalSeconds() != null) data.put("totalSeconds", request.getTotalSeconds());

        roomService.broadcastEvent(request.getRoomCode(), RoomEventType.TIMER_STARTED,
                userId, null, data);
    }

    @MessageMapping("/room.timerCompleted")
    public void handleTimerCompleted(RoomEventRequest request) {
        String userId = getUserId();
        if (request.getRoomCode() == null) return;
        if (!roomService.isParticipant(request.getRoomCode(), userId)) return;
        if (roomService.isSpectator(request.getRoomCode(), userId)) return; // Spectators can't complete timers

        Map<String, Object> data = new HashMap<>();
        if (request.getStepNumber() != null) data.put("stepNumber", request.getStepNumber());

        roomService.broadcastEvent(request.getRoomCode(), RoomEventType.TIMER_COMPLETED,
                userId, null, data);
    }

    @MessageMapping("/room.reaction")
    public void handleReaction(RoomEventRequest request) {
        String userId = getUserId();
        if (request.getRoomCode() == null || request.getEmoji() == null) return;
        if (!roomService.isParticipant(request.getRoomCode(), userId)) return;

        roomService.broadcastEvent(request.getRoomCode(), RoomEventType.REACTION,
                userId, null,
                Map.of("emoji", request.getEmoji()));
    }

    @MessageMapping("/room.sessionCompleted")
    public void handleSessionCompleted(RoomEventRequest request) {
        String userId = getUserId();
        if (request.getRoomCode() == null) return;
        if (!roomService.isParticipant(request.getRoomCode(), userId)) return;
        if (roomService.isSpectator(request.getRoomCode(), userId)) return; // Spectators can't complete sessions

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        if (request.getRating() != null) data.put("rating", request.getRating());

        roomService.broadcastEvent(request.getRoomCode(), RoomEventType.SESSION_COMPLETED,
                userId, null, data);
    }

    private String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
