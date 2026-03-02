package com.chefkix.social.chat.dto.response;

import java.time.Instant;

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
}
