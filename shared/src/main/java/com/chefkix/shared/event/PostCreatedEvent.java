package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a new post is created.
 * <p>
 * Producer: social module (post). Consumer: identity module (profile post count).
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("POST_CREATED_ACTION")
public class PostCreatedEvent extends BaseEvent {

    private String postId;

    @Builder
    public PostCreatedEvent(String userId, String postId) {
        super("POST_CREATED_ACTION", userId);
        this.postId = postId;
    }
}
