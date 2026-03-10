package com.chefkix.social.moderation.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response for ban information shown to banned users and admins.
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
