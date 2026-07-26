package com.chefkix.culinary.features.room.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
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
