package com.chefkix.culinary.features.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoomRequest {
    @NotBlank(message = "recipeId must not be blank")
    String recipeId;
}
