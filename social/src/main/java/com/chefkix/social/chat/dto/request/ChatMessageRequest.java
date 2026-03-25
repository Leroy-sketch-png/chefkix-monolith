package com.chefkix.social.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @NotBlank
    @Size(max = 5000)
    String message;

    @NotNull
    MessageType type;
    String relatedId;

    /** ID of the message being replied to (null if not a reply) */
    String replyToId;
}
