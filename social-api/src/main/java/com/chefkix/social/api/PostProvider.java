package com.chefkix.social.api;

import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.api.dto.PostSummary;
import com.chefkix.social.api.dto.RecentCookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cross-module contract for post/feed operations.
 * <p>
 * Implemented by {@code social} module, consumed by {@code identity}, {@code culinary} modules.
 * Replaces the old Feign clients: PostClient (in identity, recipe, chat services).
 */
public interface PostProvider {

    /**
     * Get paginated posts by a specific user.
     * Replaces: {@code GET /posts/user/{userId}} (post-service endpoint consumed by identity-service).
     *
     * @param userId   the author's user ID
     * @param pageable pagination parameters
     * @return page of post summaries
     */
    Page<PostSummary> getPostsByUserId(String userId, Pageable pageable);

    /**
     * Count total posts by a specific user.
     * Replaces: {@code GET /posts/user/{userId}/count} (post-service endpoint consumed by identity-service).
     *
     * @param userId the author's user ID
     * @return total number of posts
     */
    long countPostsByUserId(String userId);

    /**
     * Get post linking info (for linking a cooking session to a post).
     * Replaces: {@code POST /posts/{postId}/internal/post-link}
     * (post-service's internal endpoint consumed by recipe-service).
     *
     * @param postId the post to retrieve linking info for
     * @return linking info containing postId, photoCount, userId
     */
    PostLinkInfo getPostLinking(String postId);

    /**
     * Update the XP earned on a post (after session XP calculation).
     * Replaces: {@code PATCH /posts/{postId}/internal/update-xp}
     * (post-service's internal endpoint consumed by recipe-service).
     *
     * @param postId   the post to update
     * @param xpAmount the XP amount to set
     */
    void updatePostXp(String postId, double xpAmount);

    /**
     * Get post detail for sharing in chat messages.
     * Replaces: {@code GET /posts/{postId}/internal/detail}
     * (post-service's internal endpoint consumed by chat-service).
     *
     * @param postId the post ID
     * @return post detail with recipe context, or null if not found
     */
    PostDetail getPostDetail(String postId);

    /**
     * Auto-create a lightweight RECENT_COOK post after cooking session completion.
     * The post has no photos — just metadata (recipe title, duration, cover image).
     * Visible to followers in the feed as a compact activity item.
     *
     * @param request the recent cook details (userId, sessionId, recipeId, recipeTitle, etc.)
     */
    void createRecentCookPost(RecentCookRequest request);
}
