package com.chefkix.social.chat.controller;

import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.request.ChatReactionRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
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
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid ChatMessageRequest request) {
        ChatMessageResponse response = chatMessageService.create(request);

        messagingTemplate.convertAndSend("/topic/conversation/" + request.getConversationId(), response);

        log.info("Sent message to /topic/conversation/{}", request.getConversationId());
    }

    /**
     */
    @MessageMapping("/chat.react.{messageId}")
    public void reactToMessage(@DestinationVariable String messageId, @Valid ChatReactionRequest request) {
        ChatMessageResponse response = chatMessageService.reactToMessage(messageId, request);

        messagingTemplate.convertAndSend("/topic/conversation/" + response.getConversationId(), response);

        log.info("Broadcast reaction on message {} to /topic/conversation/{}", messageId, response.getConversationId());
    }

    /**
     */
    @MessageMapping("/chat.delete.{messageId}")
    public void deleteMessage(@DestinationVariable String messageId) {
        ChatMessageResponse response = chatMessageService.deleteMessage(messageId);

        messagingTemplate.convertAndSend("/topic/conversation/" + response.getConversationId(), response);

        log.info("Broadcast message delete {} to /topic/conversation/{}", messageId, response.getConversationId());
    }
}
