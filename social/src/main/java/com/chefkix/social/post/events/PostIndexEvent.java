package com.chefkix.social.post.events;

import com.chefkix.social.post.entity.Post;

/**
 * Spring Application Event fired after a post lifecycle change that requires
 * Typesense re-indexing. Uses the application event bus (same JVM) -- no Kafka needed.
 *
 * Consumed by TypesenseDataSyncer in the application module.
 *
 * action = "INDEX"  -> upsert post document in Typesense
 * action = "REMOVE" -> delete post document from Typesense
 */
public record PostIndexEvent(Post post, String action, String postId) {

    /** Index a just-created post. */
    public static PostIndexEvent index(Post post) {
        return new PostIndexEvent(post, "INDEX", post.getId());
    }

    /** Remove a post that was deleted. */
    public static PostIndexEvent remove(String postId) {
        return new PostIndexEvent(null, "REMOVE", postId);
    }
}
