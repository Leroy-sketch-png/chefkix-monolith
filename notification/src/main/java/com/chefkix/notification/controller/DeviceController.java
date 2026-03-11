package com.chefkix.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.chefkix.notification.dto.request.RegisterPushTokenRequest;
import com.chefkix.notification.service.PushNotificationService;
import com.chefkix.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for device push token management.
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Validated
@Tag(name = "Device Push Tokens", description = "FCM token registration for push notifications")
public class DeviceController {

    private final PushNotificationService pushNotificationService;

    @PostMapping("/push-token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register FCM push token",
            description = "Register or update a device's FCM token for push notifications")
    public ResponseEntity<ApiResponse<String>> registerPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegisterPushTokenRequest request) {

        String userId = jwt.getSubject();
        pushNotificationService.registerToken(userId, request);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .statusCode(200)
                .data("Push token registered successfully")
                .build());
    }

    @DeleteMapping("/push-token/{deviceId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unregister push token",
            description = "Unregister a specific device's push token")
    public ResponseEntity<ApiResponse<String>> unregisterPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String deviceId) {

        String userId = jwt.getSubject();
        pushNotificationService.unregisterToken(userId, deviceId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .statusCode(200)
                .data("Push token unregistered")
                .build());
    }

    @DeleteMapping("/push-tokens")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unregister all push tokens",
            description = "Unregister all push tokens for the current user (logout from all devices)")
    public ResponseEntity<ApiResponse<String>> unregisterAllPushTokens(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        pushNotificationService.unregisterAllTokens(userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .statusCode(200)
                .data("All push tokens unregistered")
                .build());
    }
}
