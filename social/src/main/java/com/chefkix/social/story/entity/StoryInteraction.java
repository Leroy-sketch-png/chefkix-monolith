package com.chefkix.social.story.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "story_interactions")
@CompoundIndex(def = "{'storyId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryInteraction {
    @Id String id;
    String storyId;
    String userId;

    boolean isViewed;
    Instant lastViewedAt;

    String reaction;

    @Builder.Default
    Map<String, String> stickerInteractions = new HashMap<>();
}