package com.chefkix.identity.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletionRequest {

    String userId;

    int xpAmount;

    String recipeId;

    String sessionId;

    boolean challengeCompleted;

    List<String> newBadges;

    /**
     */
    String idempotencyKey;
}
