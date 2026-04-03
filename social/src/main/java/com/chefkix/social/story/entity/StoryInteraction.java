package com.chefkix.social.story.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "story_interactions")
// Đảm bảo 1 user chỉ có 1 record tương tác cho 1 story
@CompoundIndex(def = "{'storyId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryInteraction {
    @Id String id;
    String storyId;
    String userId;

    // View data
    boolean isViewed;
    Instant lastViewedAt;

    // Reaction data (🔥, ❤️, 🤤)
    String reaction;

    // Sticker data (Lưu kết quả vote Poll hoặc trả lời Q&A)
    // Key: stickerId, Value: Lựa chọn của user
    @Builder.Default
    Map<String, String> stickerInteractions = new HashMap<>();
}