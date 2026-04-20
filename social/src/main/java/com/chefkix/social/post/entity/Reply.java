package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "reply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    @Id String id;

    // --- Reply author information ---
    String userId;
    String displayName;
    String avatarUrl;
    // ---------------------------------

    String content;
    @Builder.Default Integer likes = 0;
    @Builder.Default Integer comments = 0;

    @CreatedDate Instant createdAt;
    @LastModifiedDate Instant updatedAt;

    List<String> taggedUserIds;

    @Indexed String parentCommentId;

}
