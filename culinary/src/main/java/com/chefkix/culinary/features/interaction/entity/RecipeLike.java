package com.chefkix.culinary.features.interaction.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.Instant;

@Document(collection = "recipe_likes")
// Compound index to ensure a User can only Like a Recipe once (unique) and for fast lookups
@CompoundIndex(name = "recipe_user_idx", def = "{'recipeId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeLike {
    String id; // Auto-generated Mongo string ID
    String recipeId;
    String userId;

    @CreatedDate
    Instant createdAt;
    // Can add createdAt if sorting by like timestamp is needed
}