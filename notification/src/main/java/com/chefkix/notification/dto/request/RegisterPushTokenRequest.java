package com.chefkix.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request to register an FCM push token for the current user's device.
 */
@Data
public class RegisterPushTokenRequest {

    /** FCM registration token from Firebase SDK */
    @NotBlank(message = "FCM token is required")
    @Size(max = 500, message = "FCM token must be at most 500 characters")
    private String fcmToken;

    /** Unique device identifier (browser fingerprint, app install ID) */
    @NotBlank(message = "Device ID is required")
    @Size(max = 200, message = "Device ID must be at most 200 characters")
    private String deviceId;

    /** Platform: "web", "android", "ios" */
    @Pattern(regexp = "^(web|android|ios)$", message = "Platform must be web, android, or ios")
    private String platform = "web";

    /** Optional: Human-readable device name */
    @Size(max = 100, message = "Device name must be at most 100 characters")
    private String deviceName;
}
