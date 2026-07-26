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

    @Builder.Default
    List<String> completedRecipeIds = new ArrayList<>();

    @Builder.Default
    int currentRecipeIndex = 0;

    @Builder.Default
    int totalXpEarned = 0;

    @CreatedDate
    Instant startedAt;

    @LastModifiedDate
    Instant lastActivityAt;
}
