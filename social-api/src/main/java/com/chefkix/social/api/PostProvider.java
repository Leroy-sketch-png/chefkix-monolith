package com.chefkix.social.api;

import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.api.dto.PostSummary;
import com.chefkix.social.api.dto.RecentCookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 */
public interface PostProvider {

    /**
     *
     */
    Page<PostSummary> getPostsByUserId(String userId, Pageable pageable);

    /**
     *
     */
    long countPostsByUserId(String userId);

    /**
     *
     */
    PostLinkInfo getPostLinking(String postId);

    /**
     *
     */
    void updatePostXp(String postId, double xpAmount);

    /**
     *
     */
    PostDetail getPostDetail(String postId);

    /**
     *
     */
    void createRecentCookPost(RecentCookRequest request);

    /**
     *
     */
    long cleanupDeletedUserData(String userId);
}
