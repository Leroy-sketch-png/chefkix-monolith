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
    String id; // Mongo tự sinh ID string
    String recipeId;
    String userId;
    // Có thể thêm createdAt nếu muốn sort theo thời gian like
}