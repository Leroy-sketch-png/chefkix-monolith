package com.chefkix.social.post.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded document representing one stage in a learning path's difficulty progression.
 * E.g., "Stage 1: Beginner basics (recipes 1-3)", "Stage 2: Knife skills (recipes 4-6)".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DifficultyStep {

    /** Display label, e.g., "Knife Basics", "Intermediate Sauces" */
    String label;

    /** Difficulty level of this stage: Beginner, Intermediate, Advanced, Expert */
    String difficulty;

    /** Ordered recipe IDs belonging to this stage */
    @Builder.Default
    List<String> recipeIds = new ArrayList<>();

    /** Zero-based order within the learning path */
    int order;
}
