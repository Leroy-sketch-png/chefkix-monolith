package com.chefkix.social.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.chefkix.social.chat.enums.MessageType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageRequest {
    @NotBlank
    String conversationId;

    String message;

    MessageType type;
    String relatedId;

    /** ID of the message being replied to (null if not a reply) */
    String replyToId;
}
