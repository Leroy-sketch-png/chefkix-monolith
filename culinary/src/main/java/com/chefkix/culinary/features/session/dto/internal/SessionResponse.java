package com.chefkix.culinary.features.session.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    String id;
    String userId;
    LocalDateTime completedAt;

Double pendingXp;

    String recipeId;
    String recipeTitle;
String recipeAuthorId;
Double recipeBaseXp;
}