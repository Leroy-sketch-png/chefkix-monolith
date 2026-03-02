package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a post is deleted.
 * <p>
 * Producer: social module (post). Consumer: identity module (profile post count).
 * <p>
 * NOTE: Fixed bug from chefkix-be where @JsonTypeName was incorrectly
 * set to "POST_CREATED_ACTION" instead of "POST_DELETED_ACTION".
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("POST_DELETED_ACTION")
public class PostDeletedEvent extends BaseEvent {

    private String postId;

    @Builder
    public PostDeletedEvent(String userId, String postId) {
        super("POST_DELETED_ACTION", userId);
        this.postId = postId;
    }
}
