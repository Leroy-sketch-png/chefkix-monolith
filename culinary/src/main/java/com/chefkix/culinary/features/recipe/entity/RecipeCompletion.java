package com.chefkix.culinary.features.recipe.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "recipe_completions")
@Data
@Builder
@CompoundIndex(name = "user_completed_idx", def = "{'userId': 1, 'completedAt': -1}")
public class RecipeCompletion {
    @Id
    String id;

    @Indexed
    String userId;
    @Indexed
    String recipeId;

List<String> proofImageUrls;

    int actualDurationSeconds;
int xpAwarded;
boolean isPublic;

    @Builder.Default
    Boolean isPosted = false;

    @CreatedDate
    @Indexed
    LocalDateTime completedAt;
}