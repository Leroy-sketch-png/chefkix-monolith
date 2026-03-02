package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Tracks which users have liked which comments.
 * Compound index ensures one like per user per comment.
 */
@Document(collection = "comment_likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@CompoundIndex(def = "{'commentId': 1, 'userId': 1}", unique = true)
public class CommentLike {
    @Id
    String id;

    String commentId;
    String userId;

    @CreatedDate
    Instant createdAt;
}
