package com.chefkix.culinary.features.room.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 * Represents a participant in a cooking room.
 * Stored as part of CookingRoom in Redis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomParticipant {
    String userId;
    String displayName;
    String avatarUrl;
    String sessionId;
    int currentStep;
    List<Integer> completedSteps;
    Instant joinedAt;
    boolean isHost;

    /** COOK (default) or SPECTATOR — spec 24-advanced-multiplayer.txt §3 */
    @Builder.Default
    String role = "COOK";
}
