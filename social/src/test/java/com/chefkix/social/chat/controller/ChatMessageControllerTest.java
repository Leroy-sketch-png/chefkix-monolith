package com.chefkix.social.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.enums.MessageType;
import com.chefkix.social.chat.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {
    @Mock ChatMessageService chatMessageService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void restCreateBroadcastsTheAuthoritativeMessage() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId("conversation-1")
                .message("water is boiling")
                .type(MessageType.TEXT)
                .clientMessageId("client-1")
                .build();
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id("message-1")
                .conversationId("conversation-1")
                .clientMessageId("client-1")
                .build();
        when(chatMessageService.create(request)).thenReturn(response);
        ChatMessageController controller =
                new ChatMessageController(chatMessageService, messagingTemplate);

        var result = controller.create(request);

        assertThat(result.getData()).isEqualTo(response);
        verify(messagingTemplate)
                .convertAndSend("/topic/conversation/conversation-1", response);
    }
}
