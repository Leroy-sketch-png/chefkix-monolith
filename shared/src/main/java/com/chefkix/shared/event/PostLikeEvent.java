package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a user likes a post.
 * <p>
 * Producer: social module (post). Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("POST_LIKE_ACTION")
public class PostLikeEvent extends BaseEvent {

    private String postId;
    private String likerId;
    private String postOwnerId;
    private String displayName;
    private String likerAvatarUrl;
    private String content;

    @Builder
    public PostLikeEvent(String postId, String likerId, String postOwnerId,
                         String displayName, String likerAvatarUrl, String content) {
        super("POST_LIKE_ACTION", postOwnerId);
        this.postId = postId;
        this.likerId = likerId;
        this.postOwnerId = postOwnerId;
        this.displayName = displayName;
        this.likerAvatarUrl = likerAvatarUrl;
        this.content = content;
    }
}
