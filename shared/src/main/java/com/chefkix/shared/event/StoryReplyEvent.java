package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("STORY_REPLIED")
public class StoryReplyEvent extends BaseEvent {

    private String storyId;
    private String storyOwnerId; // Story owner (will receive the chat message)
    private String replierId;    // Viewer who replied to the story
    private String replyText;
    private String storyMediaUrl; // Story media URL for chat thumbnail

    @Builder
    public StoryReplyEvent(String storyId, String storyOwnerId, String replierId, String replyText, String storyMediaUrl) {
        super("STORY_REPLIED", replierId);
        this.storyId = storyId;
        this.storyOwnerId = storyOwnerId;
        this.replierId = replierId;
        this.replyText = replyText;
        this.storyMediaUrl = storyMediaUrl;
    }
}