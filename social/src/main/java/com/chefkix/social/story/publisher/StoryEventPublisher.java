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

    @Async
    public void publishStoryInteractionEvent(String storyId, String storyOwnerId, String actorId, String interactionType) {
        try {
            BasicProfileInfo profile = null;
            try {
                profile = profileProvider.getBasicProfile(actorId);
            } catch (Exception e) {
                log.warn("Could not fetch profile for user {}: {}", actorId, e.getMessage());
            }

            String displayName = profile != null ? profile.getDisplayName() : "A user";
            String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

            StoryInteractionEvent event = StoryInteractionEvent.builder()
                    .storyId(storyId)
                    .storyOwnerId(storyOwnerId)
                    .userId(actorId)
                    .userDisplayName(displayName)
                    .userAvatarUrl(avatarUrl)
                    .interactionType(interactionType)
                    .build();

            // Fire-and-forget via Kafka
            kafkaTemplate.send("story-delivery", event);

            log.info("Published STORY_INTERACTED ({}) for story {}", interactionType, storyId);

        } catch (Exception e) {
            log.error("Failed to publish story interaction event", e);
        }
    }

    // Story reply via chat message
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

            kafkaTemplate.send("chat-delivery", event);
            log.info("Published STORY_REPLIED event for story {}", storyId);

        } catch (Exception e) {
            log.error("Failed to publish story reply event", e);
        }
    }
}