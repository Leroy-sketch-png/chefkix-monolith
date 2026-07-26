package com.chefkix.culinary.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionInfo {

    String id;

    String userId;

    String status;

    LocalDateTime completedAt;

    Double pendingXp;

    String recipeId;

    String recipeTitle;

    String recipeAuthorId;

    Double recipeBaseXp;

String roomCode;
}
