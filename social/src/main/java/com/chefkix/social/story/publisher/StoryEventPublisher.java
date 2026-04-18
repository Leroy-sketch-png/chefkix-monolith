package com.chefkix.social.story.publisher;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.StoryInteractionEvent;
import com.chefkix.shared.event.StoryReplyEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryEventPublisher {

    KafkaTemplate<String, Object> kafkaTemplate;
    ProfileProvider profileProvider;

    // Chỉ cần @Async, Spring sẽ tự vứt hàm này cho Virtual Thread chạy ngầm
    @Async
    public void publishStoryInteractionEvent(String storyId, String storyOwnerId, String actorId, String interactionType) {
        try {
            // 1. Lấy thông tin (Cứ gọi thẳng, Virtual Thread block thoải mái không sợ tốn tài nguyên)
            BasicProfileInfo profile = null;
            try {
                profile = profileProvider.getBasicProfile(actorId);
            } catch (Exception ignored) {
                log.warn("Could not fetch profile for user {}", actorId);
            }

            String displayName = profile != null ? profile.getDisplayName() : "Một người dùng";
            String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

            // 2. Tạo Event
            StoryInteractionEvent event = StoryInteractionEvent.builder()
                    .storyId(storyId)
                    .storyOwnerId(storyOwnerId)
                    .userId(actorId)
                    .userDisplayName(displayName)
                    .userAvatarUrl(avatarUrl)
                    .interactionType(interactionType)
                    .build();

            // 3. Bắn Kafka
            // kafkaTemplate.send() vốn trả về 1 CompletableFuture,
            // nhưng ta là luồng ngầm rồi nên cứ quăng đó không cần hứng kết quả (Fire and Forget)
            kafkaTemplate.send("story-delivery", event);

            log.info("Published STORY_INTERACTED ({}) for story {}", interactionType, storyId);

        } catch (Exception e) {
            log.error("Failed to publish story interaction event", e);
        }
    }

    // ==========================================
    // 2. EVENT: REPLY STORY BẰNG TIN NHẮN (Gửi cho Chat)
    // ==========================================
    @Async
    public void publishStoryReplyEvent(String storyId, String storyOwnerId, String replierId, String text, String mediaUrl) {
        try {
            StoryReplyEvent event = StoryReplyEvent.builder()
                    .storyId(storyId)
                    .storyOwnerId(storyOwnerId)
                    .replierId(replierId)
                    .replyText(text)
                    .storyMediaUrl(mediaUrl)
                    .build();

            // Gửi vào topic riêng cho module Chat
            kafkaTemplate.send("chat-delivery", event);
            log.info("Published STORY_REPLIED event for story {}", storyId);

        } catch (Exception e) {
            log.error("Failed to publish story reply event", e);
        }
    }
}