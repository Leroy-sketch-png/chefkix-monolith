package com.chefkix.culinary.features.room.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ephemeral cooking room stored in Redis (NOT MongoDB).
 * TTL: 4 hours. Auto-deleted when expired or all participants leave.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CookingRoom {
    String roomCode;
    String recipeId;
    String recipeTitle;
    String hostUserId;
    String status; // WAITING, COOKING, DISSOLVED
    @Builder.Default int maxParticipants = 6;
    Instant createdAt;
    @Builder.Default List<RoomParticipant> participants = new ArrayList<>();

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_COOKING = "COOKING";
    public static final String STATUS_DISSOLVED = "DISSOLVED";
}
