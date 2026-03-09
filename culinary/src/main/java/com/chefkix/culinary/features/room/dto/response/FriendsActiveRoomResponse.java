package com.chefkix.culinary.features.room.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Active cooking room visible to a user's friends.
 * Spec: vision_and_spec/24-advanced-multiplayer.txt §5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendsActiveRoomResponse {
    String roomCode;
    String recipeId;
    String recipeTitle;
    int participantCount;
    List<String> participantNames;
    long startedMinutesAgo;
}
