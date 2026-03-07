package com.chefkix.culinary.features.room.dto.response;

import com.chefkix.culinary.features.room.model.RoomParticipant;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CookingRoomResponse {
    String roomCode;
    String recipeId;
    String recipeTitle;
    String hostUserId;
    String status;
    int maxParticipants;
    List<RoomParticipant> participants;
    Instant createdAt;
    /** The joining user's cooking session ID (only set on create/join) */
    String sessionId;
}
