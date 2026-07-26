package com.chefkix.social.chat.listener;

import com.chefkix.shared.event.BaseEvent;
import com.chefkix.shared.event.StoryReplyEvent;
import com.chefkix.social.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatMessageService chatMessageService;

    /**
     */
    @KafkaListener(topics = "chat-delivery", groupId = "chat-service-group")
    public void handleChatEvents(BaseEvent event) {
        log.debug("Received event {} with ID {}", event.getEventType(), event.getEventId());

        try {
            if (event instanceof StoryReplyEvent storyReplyEvent) {

                log.info("Processing STORY_REPLY event for story: {}", storyReplyEvent.getStoryId());
                chatMessageService.processStoryReplyEvent(storyReplyEvent);

            } else {
                log.info("Event type {} is not handled by ChatEventListener yet", event.getEventType());
            }

        } catch (Exception e) {
            log.error("Error processing event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }
}