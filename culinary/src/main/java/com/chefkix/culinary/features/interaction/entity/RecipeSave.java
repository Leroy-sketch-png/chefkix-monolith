package com.chefkix.culinary.features.interaction.entity;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Document(collection = "recipe_saves")
@CompoundIndex(name = "recipe_user_idx", def = "{'recipeId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSave {
    String id; // Auto-generated Mongo string ID
    String recipeId;
    String userId;
    // Can add createdAt if sorting by save timestamp is needed
}