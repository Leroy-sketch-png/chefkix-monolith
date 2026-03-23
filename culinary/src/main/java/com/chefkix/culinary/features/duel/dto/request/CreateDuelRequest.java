package com.chefkix.culinary.features.duel.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateDuelRequest {
    String opponentId;
    String recipeId;
    String message; // optional trash talk
}
