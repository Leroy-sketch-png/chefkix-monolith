package com.chefkix.culinary.features.interaction.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "recipe_saves")
@CompoundIndexes({
        @CompoundIndex(name = "recipe_user_idx", def = "{'recipeId': 1, 'userId': 1}", unique = true),
        @CompoundIndex(name = "user_recipe_idx", def = "{'userId': 1, 'recipeId': 1}"),
        @CompoundIndex(name = "user_createdAt_idx", def = "{'userId': 1, 'createdAt': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSave {
String id;
    String recipeId;
    String userId;

    @CreatedDate
    Instant createdAt;
}