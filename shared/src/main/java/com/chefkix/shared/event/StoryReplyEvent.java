package com.chefkix.shared.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoryReplyEvent extends BaseEvent {

    private String storyId;
    private String storyOwnerId; // Chủ nhân Story (Người sẽ nhận tin nhắn)
    private String replierId;    // Người xem Story và gõ text reply (Người gửi tin nhắn)
    private String replyText;    // Nội dung "Trông ngon quá!"
    private String storyMediaUrl;// Link ảnh/video của Story để làm Thumbnail trong Chat

    @Builder
    public StoryReplyEvent(String storyId, String storyOwnerId, String replierId, String replyText, String storyMediaUrl) {
        // Gọi constructor của BaseEvent (eventType, targetId, actorId)
        super("STORY_REPLIED", replierId);
        this.storyId = storyId;
        this.storyOwnerId = storyOwnerId;
        this.replierId = replierId;
        this.replyText = replyText;
        this.storyMediaUrl = storyMediaUrl;
    }
}