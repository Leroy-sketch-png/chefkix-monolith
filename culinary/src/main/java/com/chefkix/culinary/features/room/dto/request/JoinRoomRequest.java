package com.chefkix.culinary.features.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Builder.Default
    @Size(max = 20)
    String role = "COOK";
}
