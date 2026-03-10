package com.chefkix.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to register an FCM push token for the current user's device.
 */
@Data
public class RegisterPushTokenRequest {

    /** FCM registration token from Firebase SDK */
    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    /** Unique device identifier (browser fingerprint, app install ID) */
    @NotBlank(message = "Device ID is required")
    private String deviceId;

    /** Platform: "web", "android", "ios" */
    private String platform = "web";

    /** Optional: Human-readable device name */
    private String deviceName;
}
