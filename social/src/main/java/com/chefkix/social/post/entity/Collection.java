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

    // --- Social (post bookmarks) ---
    @Builder.Default
    List<String> postIds = new ArrayList<>();

    @Builder.Default
    int itemCount = 0;

    // --- Learning Path (recipe progression) ---
    /** Ordered recipe IDs forming the learning path curriculum. Distinct from postIds. */
    @Builder.Default
    List<String> recipeIds = new ArrayList<>();

    /** Collection type: BOOKMARK (default, post-based) or LEARNING_PATH (recipe curriculum) */
    @Builder.Default
    String collectionType = "BOOKMARK";

    /** Overall difficulty label for the learning path */
    String difficulty;

    /** Estimated total minutes to complete all recipes */
    Integer estimatedTotalMinutes;

    /** Total XP earnable by completing the full path */
    Integer totalXp;

    /** How many users enrolled in this learning path */
    @Builder.Default
    int enrolledCount = 0;

    /** Fraction of enrolled users who completed it (0.0 - 1.0) */
    Double completionRate;

    /** Average user rating (1.0 - 5.0) */
    Double averageRating;

    /** Ordered difficulty progression stages (embedded) */
    @Builder.Default
    List<DifficultyStep> difficultyProgression = new ArrayList<>();

    /** Whether this collection is admin-featured (shown on Explore). */
    @Builder.Default
    boolean isFeatured = false;

    /** Seasonal tag for time-based curation (e.g., "summer-2025", "holiday-baking"). */
    String seasonTag;

    /** Short tagline for featured display (e.g., "Summer grilling at its finest"). */
    String tagline;

    /** Emoji icon for visual flair in featured cards. */
    String emoji;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
