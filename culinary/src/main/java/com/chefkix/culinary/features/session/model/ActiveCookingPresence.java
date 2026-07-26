package com.chefkix.culinary.features.session.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveCookingPresence {
    String userId;
    String username;
    String displayName;
    String avatarUrl;
    String recipeId;
    String recipeTitle;
    List<String> coverImageUrl;
    int currentStep;
    int totalSteps;
    LocalDateTime startedAt;
String roomCode;
}
