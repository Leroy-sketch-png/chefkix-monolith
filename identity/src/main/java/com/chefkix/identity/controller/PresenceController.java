package com.chefkix.identity.controller;

import com.chefkix.identity.dto.response.PresenceResponse;
import com.chefkix.identity.service.PresenceService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    /**
     */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) HeartbeatRequest request) {
        String userId = jwt.getSubject();
        String activity = request != null ? request.activity() : "browsing";
        presenceService.heartbeat(userId, activity);
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200).build();
    }

    /**
     */
    @GetMapping("/friends")
    public ApiResponse<List<PresenceResponse>> getFriendsPresence(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ApiResponse.<List<PresenceResponse>>builder()
                .success(true).statusCode(200)
                .data(presenceService.getFriendsPresence(userId))
                .build();
    }

    /**
     */
    @GetMapping("/friends/cooking")
    public ApiResponse<List<PresenceResponse>> getFriendsCookingNow(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ApiResponse.<List<PresenceResponse>>builder()
                .success(true).statusCode(200)
                .data(presenceService.getFriendsCookingNow(userId))
                .build();
    }

    /**
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserPresenceResponse> getUserPresence(@PathVariable String userId) {
        boolean online = presenceService.isOnline(userId);
        return ApiResponse.<UserPresenceResponse>builder()
                .success(true).statusCode(200)
                .data(new UserPresenceResponse(userId, online))
                .build();
    }

    /**
     */
    @PostMapping("/offline")
    public ApiResponse<Void> goOffline(@AuthenticationPrincipal Jwt jwt) {
        presenceService.goOffline(jwt.getSubject());
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200).build();
    }


    record HeartbeatRequest(String activity) {}
    
    record UserPresenceResponse(String userId, boolean online) {}
}
