package com.chefkix.culinary.features.room.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaveRoomResponse {
    boolean left;
    boolean roomDissolved;
    /** New host user ID if host was transferred, null otherwise */
    String newHostUserId;
}
