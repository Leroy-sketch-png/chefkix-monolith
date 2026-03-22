package com.chefkix.identity.events;

import com.chefkix.identity.entity.UserProfile;

/**
 * Spring Application Event fired after a user profile change that requires
 * Typesense re-indexing. Uses the application event bus (same JVM) -- no Kafka needed.
 *
 * Consumed by TypesenseDataSyncer in the application module.
 */
public record UserIndexEvent(UserProfile profile, String action, String userId) {

    /** Index a user profile (create or update). */
    public static UserIndexEvent index(UserProfile profile) {
        return new UserIndexEvent(profile, "INDEX", profile.getUserId());
    }

    /** Remove a user from the search index. */
    public static UserIndexEvent remove(String userId) {
        return new UserIndexEvent(null, "REMOVE", userId);
    }
}
