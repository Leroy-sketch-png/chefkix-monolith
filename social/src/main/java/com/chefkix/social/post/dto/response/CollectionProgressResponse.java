package com.chefkix.social.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionProgressResponse {

    String id;
    String userId;
    String collectionId;
    List<String> completedRecipeIds;
    int currentRecipeIndex;
    int totalXpEarned;

    /** Total recipes in the learning path (for progress bar: completedRecipeIds.size / totalRecipes) */
    int totalRecipes;

    /** Computed: completedRecipeIds.size / totalRecipes */
    double progressPercent;

    Instant startedAt;
    Instant lastActivityAt;
}
