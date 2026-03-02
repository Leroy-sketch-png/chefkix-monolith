package com.chefkix.identity.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Friend (following) list for a user.
 * <p>
 * Replaces: identity's {@code InternalFriendListResponse}.
 * Used by culinary module for "friends only" recipe visibility filtering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendListInfo {

    String userId;

    List<String> friendIds;

    int totalCount;
}
