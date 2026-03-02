package com.chefkix.social.chat.entity;

import java.time.Instant;

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

    // Thêm trường loại tin nhắn
    @Builder.Default
    MessageType type = MessageType.TEXT;

    @Indexed
    String relatedId;

    String sharedPostImage;
    String sharedPostTitle;

    ParticipantInfo sender;

    @Indexed
    Instant createdDate;
}
