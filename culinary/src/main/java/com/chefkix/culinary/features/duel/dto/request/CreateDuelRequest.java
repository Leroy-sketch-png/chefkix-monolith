package com.chefkix.culinary.features.duel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateDuelRequest {
    @NotBlank(message = "Opponent ID is required")
    String opponentId;
    @NotBlank(message = "Recipe ID is required")
    String recipeId;
    @Size(max = 200, message = "Message must be 200 characters or less")
    String message; // optional trash talk
}
