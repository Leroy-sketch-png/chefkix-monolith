package com.chefkix.culinary.features.room.controller;

import com.chefkix.culinary.features.room.dto.request.RoomEventRequest;
import com.chefkix.culinary.features.room.model.RoomEventType;
import com.chefkix.culinary.features.room.service.CookingRoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookingRoomWsControllerTest {

    @Mock
    private CookingRoomService roomService;

    private CookingRoomWsController controller;

    @BeforeEach
    void setUp() {
        controller = new CookingRoomWsController(roomService);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user-1", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void broadcastsAllowlistedReactionForRoomParticipant() {
        RoomEventRequest request = reactionRequest("room-1", " 🔥 ");
        when(roomService.isParticipant("room-1", "user-1")).thenReturn(true);

        controller.handleReaction(request);

        verify(roomService).broadcastEvent(
                eq("room-1"), eq(RoomEventType.REACTION), eq("user-1"), isNull(),
                eq(Map.of("emoji", "🔥")));
    }

    @Test
    void rejectsReactionOutsideAllowlistBeforeRoomLookup() {
        RoomEventRequest request = reactionRequest("room-1", "<script>");

        controller.handleReaction(request);

        verify(roomService, never()).isParticipant("room-1", "user-1");
        verify(roomService, never()).broadcastEvent(
                eq("room-1"), eq(RoomEventType.REACTION), eq("user-1"), isNull(),
                anyMap());
    }

    @Test
    void rejectsMissingReactionBeforeRoomLookup() {
        controller.handleReaction(reactionRequest("room-1", null));

        verify(roomService, never()).isParticipant("room-1", "user-1");
        verify(roomService, never()).broadcastEvent(
                eq("room-1"), eq(RoomEventType.REACTION), eq("user-1"), isNull(),
                anyMap());
    }

    @Test
    void rejectsReactionFromNonParticipant() {
        RoomEventRequest request = reactionRequest("room-1", "👏");
        when(roomService.isParticipant("room-1", "user-1")).thenReturn(false);

        controller.handleReaction(request);

        verify(roomService, never()).broadcastEvent(
                eq("room-1"), eq(RoomEventType.REACTION), eq("user-1"), isNull(),
                anyMap());
    }

    private RoomEventRequest reactionRequest(String roomCode, String emoji) {
        RoomEventRequest request = new RoomEventRequest();
        request.setRoomCode(roomCode);
        request.setEmoji(emoji);
        return request;
    }
}
