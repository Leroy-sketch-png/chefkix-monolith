package com.chefkix.culinary.features.room.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaveActiveRoomsResponse {
    int roomsLeft;
    int roomsDissolved;
    List<String> roomCodes;
}
