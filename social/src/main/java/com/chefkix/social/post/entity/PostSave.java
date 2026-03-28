package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity representing a saved/bookmarked post.
 * Users can save posts for later viewing.
 */
@Document(collection = "post_save")
@CompoundIndexes({
    @CompoundIndex(name = "post_user_idx", def = "{'postId': 1, 'userId': 1}", unique = true),
    @CompoundIndex(name = "user_saved_feed_idx", def = "{'userId': 1, 'createdDate': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostSave {
    @Id 
    String id;

    String postId;
    String userId;

    @CreatedDate 
    LocalDateTime createdDate;
}
