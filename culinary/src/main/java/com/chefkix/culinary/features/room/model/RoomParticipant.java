package com.chefkix.culinary.features.room.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
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

    @Builder.Default
    String role = "COOK";
}
