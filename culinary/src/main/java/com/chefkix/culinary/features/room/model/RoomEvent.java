package com.chefkix.culinary.features.room.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket event broadcast to room participants.
 * Serialized as JSON and sent to /topic/room/{roomCode}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomEvent {
    RoomEventType type;
    String userId;
    String displayName;
    Instant timestamp;
    Map<String, Object> data;
}
