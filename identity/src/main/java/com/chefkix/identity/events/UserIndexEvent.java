package com.chefkix.identity.events;

import com.chefkix.identity.entity.UserProfile;

/**
 *
 */
public record UserIndexEvent(UserProfile profile, String action, String userId) {

    public static UserIndexEvent index(UserProfile profile) {
        return new UserIndexEvent(profile, "INDEX", profile.getUserId());
    }

    public static UserIndexEvent remove(String userId) {
        return new UserIndexEvent(null, "REMOVE", userId);
    }
}
