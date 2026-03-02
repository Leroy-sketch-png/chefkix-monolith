package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a user gains a new follower.
 * <p>
 * Producer: identity module. Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("NEW_FOLLOWER_ACTION")
public class NewFollowerEvent extends BaseEvent {

    private String followerId;
    private String followerDisplayName;
    private String followerAvatarUrl;
    private String followedUserId;
    private boolean isMutualFollow;

    @Builder
    public NewFollowerEvent(String followerId, String followerDisplayName,
                            String followerAvatarUrl, String followedUserId,
                            boolean isMutualFollow) {
        super("NEW_FOLLOWER_ACTION", followedUserId);
        this.followerId = followerId;
        this.followerDisplayName = followerDisplayName;
        this.followerAvatarUrl = followerAvatarUrl;
        this.followedUserId = followedUserId;
        this.isMutualFollow = isMutualFollow;
    }
}
