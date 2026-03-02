package com.chefkix.culinary.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Cooking session info exposed to other modules (social/post).
 * <p>
 * Replaces: recipe-service's {@code SessionResponse} (internal DTO) and
 * post-service's consumer copy of the same.
 * <p>
 * FIXED type mismatches:
 * <ul>
 *   <li>recipe-service's producer had {@code @Builder}, post-service's consumer did not</li>
 *   <li>post-service's consumer had extra {@code status} field → included here</li>
 *   <li>pendingXp/recipeBaseXp: both used {@code Double} → kept as-is (may be fractional from multiplier)</li>
 * </ul>
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
}
