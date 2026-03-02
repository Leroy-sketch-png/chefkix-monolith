package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Tracks which users have liked which replies.
 * Compound index ensures one like per user per reply.
 */
@Document(collection = "reply_likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@CompoundIndex(def = "{'replyId': 1, 'userId': 1}", unique = true)
public class ReplyLike {
    @Id
    String id;

    String replyId;
    String userId;

    @CreatedDate
    Instant createdAt;
}
