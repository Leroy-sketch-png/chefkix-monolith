package com.chefkix.social.story.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
    @Indexed
private String userId;
    private String title;
    private String coverUrl;
private List<String> storyIds;
    private Instant createdAt;
}