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

@Document(collection = "collections")
@CompoundIndexes({
    @CompoundIndex(name = "user_updated_idx", def = "{'userId': 1, 'updatedAt': -1}"),
    @CompoundIndex(name = "type_public_idx", def = "{'collectionType': 1, 'isPublic': 1}"),
    @CompoundIndex(name = "featured_season_idx", def = "{'isFeatured': 1, 'seasonTag': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Collection {

    @Id
    String id;

    @Indexed
    String userId;

    String name;
    String description;
    String coverImageUrl;

    @Builder.Default
    boolean isPublic = false;

    @Builder.Default
    List<String> postIds = new ArrayList<>();

    @Builder.Default
    int itemCount = 0;

    @Builder.Default
    List<String> recipeIds = new ArrayList<>();

    @Builder.Default
    String collectionType = "BOOKMARK";

    String difficulty;

    Integer estimatedTotalMinutes;

    Integer totalXp;

    @Builder.Default
    int enrolledCount = 0;

    Double completionRate;

    Double averageRating;

    @Builder.Default
    List<DifficultyStep> difficultyProgression = new ArrayList<>();

    @Builder.Default
    boolean isFeatured = false;

    String seasonTag;

    String tagline;

    String emoji;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
