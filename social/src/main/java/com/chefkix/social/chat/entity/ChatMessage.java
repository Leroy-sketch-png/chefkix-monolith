package com.chefkix.social.chat.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.chefkix.social.chat.enums.MessageType;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_message")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {
    @MongoId
    String id;

    @Indexed
    String conversationId;

    String message;

    @Builder.Default
    MessageType type = MessageType.TEXT;

    @Indexed
    String relatedId;

    String sharedPostImage;
    String sharedPostTitle;

    ParticipantInfo sender;

    @Indexed
    Instant createdDate;

    // --- Reply support ---
    /** ID of the message this is replying to (null if not a reply) */
    String replyToId;
    /** Cached snippet of the replied-to message content */
    String replyToContent;
    /** Cached sender name of the replied-to message */
    String replyToSenderName;

    // --- Reactions ---
    /** Reactions on this message. Each entry: emoji + list of userIds who reacted. */
    @Builder.Default
    List<Reaction> reactions = new ArrayList<>();

    // --- Soft delete ---
    /** True if the sender deleted this message. Content is cleared but metadata preserved. */
    @Builder.Default
    Boolean deleted = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reaction {
        String emoji;
        @Builder.Default
        List<String> userIds = new ArrayList<>();
    }
}
