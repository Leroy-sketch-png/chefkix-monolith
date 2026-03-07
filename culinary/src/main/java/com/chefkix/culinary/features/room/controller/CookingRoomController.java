package com.chefkix.culinary.features.room.controller;

import com.chefkix.culinary.features.room.dto.request.CreateRoomRequest;
import com.chefkix.culinary.features.room.dto.request.JoinRoomRequest;
import com.chefkix.culinary.features.room.dto.response.CookingRoomResponse;
import com.chefkix.culinary.features.room.dto.response.LeaveRoomResponse;
import com.chefkix.culinary.features.room.service.CookingRoomService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for cooking room lifecycle (create/join/leave/get).
 * WebSocket events are handled by {@link CookingRoomWsController}.
 */
@RestController
@RequestMapping("/cooking-rooms")
@RequiredArgsConstructor
public class CookingRoomController {

    private final CookingRoomService roomService;

    @PostMapping
    public ApiResponse<CookingRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        String userId = getUserId();
        return ApiResponse.success(roomService.createRoom(userId, request));
    }

    @PostMapping("/join")
    public ApiResponse<CookingRoomResponse> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        String userId = getUserId();
        return ApiResponse.success(roomService.joinRoom(userId, request));
    }

    @PostMapping("/{roomCode}/leave")
    public ApiResponse<LeaveRoomResponse> leaveRoom(@PathVariable String roomCode) {
        String userId = getUserId();
        return ApiResponse.success(roomService.leaveRoom(userId, roomCode));
    }

    @GetMapping("/{roomCode}")
    public ApiResponse<CookingRoomResponse> getRoom(@PathVariable String roomCode) {
        String userId = getUserId();
        return ApiResponse.success(roomService.getRoom(userId, roomCode));
    }

    private String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
