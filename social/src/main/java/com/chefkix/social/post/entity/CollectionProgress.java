package com.chefkix.social.post.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Tracks a user's progress through a learning-path Collection.
 * One document per (userId, collectionId) pair.
 */
@Document(collection = "collection_progress")
@CompoundIndexes({
    @CompoundIndex(name = "user_collection_idx", def = "{'userId': 1, 'collectionId': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionProgress {

    @Id
    String id;

    @Indexed
    String userId;

    @Indexed
    String collectionId;

    /** Recipe IDs the user has finished cooking (subset of Collection.recipeIds) */
    @Builder.Default
    List<String> completedRecipeIds = new ArrayList<>();

    /** Zero-based index into Collection.recipeIds indicating next recipe */
    @Builder.Default
    int currentRecipeIndex = 0;

    /** Total XP earned from recipes completed within this learning path */
    @Builder.Default
    int totalXpEarned = 0;

    @CreatedDate
    Instant startedAt;

    @LastModifiedDate
    Instant lastActivityAt;
}
