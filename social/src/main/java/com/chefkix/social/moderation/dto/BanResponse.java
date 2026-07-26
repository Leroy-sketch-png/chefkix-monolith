package com.chefkix.social.moderation.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BanResponse {
    String id;
    String userId;
    String reason;
    String scope;
    int durationDays;
    int offenseNumber;
    Instant issuedAt;
    Instant expiresAt;
    boolean active;
    boolean permanent;
}
