package com.chefkix.social.post.events;

import com.chefkix.social.post.entity.Post;

/**
 *
 *
 */
public record PostIndexEvent(Post post, String action, String postId) {

    public static PostIndexEvent index(Post post) {
        return new PostIndexEvent(post, "INDEX", post.getId());
    }

    public static PostIndexEvent remove(String postId) {
        return new PostIndexEvent(null, "REMOVE", postId);
    }
}
