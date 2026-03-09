package com.chefkix.culinary.features.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JoinRoomRequest {
    @NotBlank(message = "roomCode must not be blank")
    String roomCode;

    /** "COOK" (default) or "SPECTATOR". Spectators can watch but not interact with cooking steps. */
    @Builder.Default
    String role = "COOK";
}
