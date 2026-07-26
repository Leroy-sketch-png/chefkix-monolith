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

    String storyOwnerId;
    String sharedPostImage;
    String sharedPostTitle;

    ParticipantInfo sender;

    @Indexed
    Instant createdDate;

    String replyToId;
    String replyToContent;
    String replyToSenderName;

    @Builder.Default
    List<Reaction> reactions = new ArrayList<>();

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
