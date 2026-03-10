package com.chefkix.notification.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Stores FCM (Firebase Cloud Messaging) push notification tokens.
 * Each device has a unique token, but users can have multiple devices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "push_tokens")
@CompoundIndex(name = "user_device_idx", def = "{'userId': 1, 'deviceId': 1}", unique = true)
public class PushToken {

    @Id
    String id;

    /** User who owns this device */
    @Indexed
    String userId;

    /** Unique device identifier (browser fingerprint or app install ID) */
    String deviceId;

    /** FCM registration token - this changes when refreshed */
    @Indexed(unique = true)
    String fcmToken;

    /** Device platform: "web", "android", "ios" */
    String platform;

    /** Optional: device name/model for user's token management UI */
    String deviceName;

    /** Whether this token is still valid (set to false when FCM returns invalid) */
    @Builder.Default
    boolean active = true;

    /** Last time we successfully sent a push to this token */
    Instant lastUsedAt;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
