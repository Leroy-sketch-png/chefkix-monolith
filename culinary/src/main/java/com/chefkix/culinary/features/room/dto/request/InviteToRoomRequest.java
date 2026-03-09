package com.chefkix.culinary.features.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Invite a user to an active cooking room.
 * Spec: vision_and_spec/24-advanced-multiplayer.txt §2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteToRoomRequest {
    @NotBlank
    String userId;
}
