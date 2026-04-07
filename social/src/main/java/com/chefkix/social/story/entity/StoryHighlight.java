package com.chefkix.social.story.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "story_highlights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryHighlight {
    @Id
    private String id;
    private String userId; // Chủ nhân của bộ sưu tập này
    private String title;
    private String coverUrl;
    private List<String> storyIds; // Lưu mảng ID của các Story gốc
    private Instant createdAt;
}