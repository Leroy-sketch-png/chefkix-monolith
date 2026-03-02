package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a comment is posted.
 * <p>
 * Producer: social module (post/comment). Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("COMMENT_ACTION")
public class CommentEvent extends BaseEvent {

    private String postId;
    private String commentId;
    private String commenterId;
    private String commenterDisplayName;
    private String commenterAvatarUrl;
    private String postOwnerId;
    private String contentPreview;

    @Builder
    public CommentEvent(String postId, String commentId, String commenterId,
                        String commenterDisplayName, String commenterAvatarUrl,
                        String postOwnerId, String contentPreview) {
        super("COMMENT_ACTION", postOwnerId);
        this.postId = postId;
        this.commentId = commentId;
        this.commenterId = commenterId;
        this.commenterDisplayName = commenterDisplayName;
        this.commenterAvatarUrl = commenterAvatarUrl;
        this.postOwnerId = postOwnerId;
        this.contentPreview = contentPreview;
    }
}
