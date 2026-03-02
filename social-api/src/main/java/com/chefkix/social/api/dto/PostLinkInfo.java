package com.chefkix.social.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Post linking info returned after linking a cooking session to a post.
 * <p>
 * Unifies: recipe-service's {@code PostLinkingResponse} and post-service's copy (identical).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostLinkInfo {

    String postId;

    int photoCount;

    String userId;
}
