package com.chefkix.social.chat.controller;

import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatWebSocketController {

    ChatMessageService chatMessageService;
    SimpMessagingTemplate messagingTemplate;

    /**
     * Khi frontend gửi tin nhắn qua endpoint /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid ChatMessageRequest request) {
        ChatMessageResponse response = chatMessageService.create(request);

        // Gửi lại message tới topic của conversationId để FE lắng nghe realtime
        messagingTemplate.convertAndSend("/topic/conversation/" + request.getConversationId(), response);

        log.info("Sent message to /topic/conversation/{}", request.getConversationId());
    }
}
