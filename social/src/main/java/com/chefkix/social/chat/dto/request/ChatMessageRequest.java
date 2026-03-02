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
}
