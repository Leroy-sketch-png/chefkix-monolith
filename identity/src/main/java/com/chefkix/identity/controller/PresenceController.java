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
     * POST /api/v1/presence/heartbeat — Send a heartbeat to indicate user is online.
     * Body: { "activity": "browsing" | "cooking:Recipe Title" | "creating" }
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
     * GET /api/v1/presence/friends — Online friends of the authenticated user.
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
     * GET /api/v1/presence/friends/cooking — Friends who are currently cooking.
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
     * GET /api/v1/presence/{userId} — Check if a specific user is online.
     * Returns online status for any user (public profile info).
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
     * POST /api/v1/presence/offline — Explicitly go offline (logout/tab close).
     */
    @PostMapping("/offline")
    public ApiResponse<Void> goOffline(@AuthenticationPrincipal Jwt jwt) {
        presenceService.goOffline(jwt.getSubject());
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200).build();
    }

    // ── Request/Response DTOs ───────────────────────────────────────

    record HeartbeatRequest(String activity) {}
    
    record UserPresenceResponse(String userId, boolean online) {}
}
