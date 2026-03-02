package com.chefkix.social.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Post detail for sharing context in chat messages.
 * <p>
 * Replaces: chat-service's {@code InternalPostResponse}.
 * Contains just enough data to render a shared post preview in chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostDetail {

    String id;

    /** Display name of the post author */
    String displayName;

    /** Post caption/content */
    String content;

    /** Photo URLs (first element used as thumbnail in chat) */
    List<String> photoUrls;

    // --- Recipe context ---

    String recipeTitle;

    String recipeId;

    String sessionId;

    boolean privateRecipe;
}
