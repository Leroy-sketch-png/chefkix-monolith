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
}
