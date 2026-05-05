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
    String mediaUrl; // Ảnh hoặc Video nền
    String mediaType; // IMAGE hoặc VIDEO
    Double imageScale;    // Tỷ lệ phóng to của ảnh nền
    Double imageRotation;

    // Danh sách các "lớp phủ" trên Story (Text, Hashtag, Location, Link, Sticker)
    List<StoryItem> items;
    //Boolean isCloseFriendsOnly;

    // Các liên kết đặc thù cho MXH nấu ăn
    String recipeId; // Link nhanh tới công thức nấu ăn

    Instant createdAt;
    @Indexed(expireAfter = "0s")
    Instant expiresAt;

    Boolean isDeleted;
}

