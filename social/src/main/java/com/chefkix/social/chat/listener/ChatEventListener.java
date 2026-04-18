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
     * Lắng nghe tất cả các event đổ vào topic 'chat-delivery'
     */
    @KafkaListener(topics = "chat-delivery", groupId = "chat-service-group")
    public void handleChatEvents(BaseEvent event) {
        log.debug("Received event {} with ID {}", event.getEventType(), event.getEventId());

        try {
            // Sử dụng Pattern Matching của Java để tự động ép kiểu nếu đúng là StoryReplyEvent
            if (event instanceof StoryReplyEvent storyReplyEvent) {

                log.info("Processing STORY_REPLY event for story: {}", storyReplyEvent.getStoryId());
                chatMessageService.processStoryReplyEvent(storyReplyEvent);

            } else {
                // Mở rộng cho tương lai: Nếu sau này có PostShareEvent, RecipeShareEvent...
                // thì bạn chỉ cần viết thêm else if vào đây.
                log.info("Event type {} is not handled by ChatEventListener yet", event.getEventType());
            }

        } catch (Exception e) {
            // Bắt lỗi tại đây để Kafka không bị kẹt (Poison Pill) nếu có 1 event bị lỗi logic
            log.error("Error processing event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }
}