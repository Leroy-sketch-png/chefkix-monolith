package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
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
