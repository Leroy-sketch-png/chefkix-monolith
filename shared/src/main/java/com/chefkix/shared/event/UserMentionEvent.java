package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a user is @mentioned in a post or comment.
 * <p>
 * Producer: social module (post/comment). Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("USER_MENTION")
public class UserMentionEvent extends BaseEvent {

    private String sourceId;
    /** "COMMENT" or "POST" */
    private String sourceType;
    private String postId;
    private String actorId;
    private String actorDisplayName;
    private String actorAvatarUrl;
    private String contentPreview;

    @Builder
    public UserMentionEvent(String recipientId, String sourceId, String sourceType,
                            String postId, String actorId, String actorDisplayName,
                            String actorAvatarUrl, String contentPreview) {
        super("USER_MENTION", recipientId);
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.postId = postId;
        this.actorId = actorId;
        this.actorDisplayName = actorDisplayName;
        this.actorAvatarUrl = actorAvatarUrl;
        this.contentPreview = contentPreview;
    }
}
