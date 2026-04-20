package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("STORY_INTERACTED")
public class StoryInteractionEvent extends BaseEvent {

    private String storyId;
    private String storyOwnerId;
    private String userId;
    private String userDisplayName;
    private String userAvatarUrl;

    private String interactionType; // "VIEW", "LIKE", "REACTION"

    @Builder
    public StoryInteractionEvent(String storyId, String userId, String storyOwnerId, String userDisplayName, String userAvatarUrl, String interactionType) {
        super("STORY_INTERACTED", userId);

        this.storyId = storyId;
        this.storyOwnerId = storyOwnerId;
        this.userId = userId;
        this.userDisplayName = userDisplayName;
        this.userAvatarUrl = userAvatarUrl;
        this.interactionType = interactionType;
    }
}