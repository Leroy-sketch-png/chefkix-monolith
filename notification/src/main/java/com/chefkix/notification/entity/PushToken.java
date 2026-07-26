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

    @Indexed
    String userId;

    String deviceId;

    @Indexed(unique = true)
    String fcmToken;

    String platform;

    String deviceName;

    @Builder.Default
    boolean active = true;

    Instant lastUsedAt;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
