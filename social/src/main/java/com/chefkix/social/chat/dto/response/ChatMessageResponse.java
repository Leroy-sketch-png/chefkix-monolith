package com.chefkix.social.chat.dto.response;

import java.time.Instant;
import java.util.List;

import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.social.chat.enums.MessageType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    String id;
    String conversationId;
    boolean me;
    String message;
    ParticipantInfo sender;
    Instant createdDate;

    MessageType type;
    String relatedId;
    String sharedPostImage;
    String sharedPostTitle;

    // Reply context
    ReplyInfo replyTo;

    // Reactions
    List<ReactionInfo> reactions;

    // Soft delete
    Boolean deleted;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReplyInfo {
        String messageId;
        String content;
        String senderName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReactionInfo {
        String emoji;
        int count;
        boolean userReacted;
    }
}
