package com.chefkix.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 */
@Data
public class RegisterPushTokenRequest {

    @NotBlank(message = "FCM token is required")
    @Size(max = 500, message = "FCM token must be at most 500 characters")
    private String fcmToken;

    @NotBlank(message = "Device ID is required")
    @Size(max = 200, message = "Device ID must be at most 200 characters")
    private String deviceId;

    @Pattern(regexp = "^(web|android|ios)$", message = "Platform must be web, android, or ios")
    private String platform = "web";

    @Size(max = 100, message = "Device name must be at most 100 characters")
    private String deviceName;
}
