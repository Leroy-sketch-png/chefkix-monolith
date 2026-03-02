package com.chefkix.social.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 * Post summary for cross-module usage (profile page, feed display).
 * <p>
 * Mirrors post-service's {@code PostResponse} — all fields needed for rendering
 * a post card in the identity module's profile page or any other module.
 * <p>
 * Kept as a cross-module contract DTO. The social module's internal PostResponse
 * may have additional fields or differ in naming for its own controllers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostSummary {

    String id;

    String userId;

    String displayName;

    String avatarUrl;

    String content;

    String slug;

    List<String> photoUrls;

    String videoUrl;

    String postUrl;

    List<String> tags;

    // --- Gamification & session context ---

    String sessionId;

    String recipeId;

    String recipeTitle;

    @JsonProperty("isPrivateRecipe")
    boolean privateRecipe;

    Double xpEarned;

    // --- Engagement metrics ---

    Integer likes;

    Integer commentCount;

    // --- Current-user-specific flags (populated per request) ---

    @JsonProperty("isLiked")
    Boolean liked;

    @JsonProperty("isSaved")
    Boolean saved;

    // --- Timestamps ---

    Instant createdAt;

    Instant updatedAt;
}
