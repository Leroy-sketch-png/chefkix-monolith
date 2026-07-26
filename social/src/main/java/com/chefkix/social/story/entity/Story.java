package com.chefkix.social.story.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "stories")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Story {
    @Id
    String id;
    String userId;
String mediaUrl;
String mediaType;
Double imageScale;
    Double imageRotation;

    List<StoryItem> items;

String recipeId;

    Instant createdAt;
    @Indexed(expireAfter = "0s")
    Instant expiresAt;

    Boolean isDeleted;
}

