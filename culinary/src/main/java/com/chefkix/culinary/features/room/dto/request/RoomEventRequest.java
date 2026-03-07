package com.chefkix.culinary.features.room.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Generic room event request from WebSocket clients.
 * The "type" field is determined by the @MessageMapping destination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomEventRequest {
    String roomCode;
    Integer stepNumber;
    List<Integer> completedSteps;
    Integer totalSeconds;
    String emoji;
    Integer rating;
}
