package com.chefkix.culinary.features.room.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
    @Size(max = 20, message = "Room code must be at most 20 characters")
    String roomCode;

    @Min(0) @Max(100)
    Integer stepNumber;

    @Size(max = 100, message = "Maximum 100 completed steps")
    List<Integer> completedSteps;

    @Min(0) @Max(86400)
    Integer totalSeconds;

    @Size(max = 20, message = "Emoji must be at most 20 characters")
    String emoji;

    @Min(1) @Max(5)
    Integer rating;
}
