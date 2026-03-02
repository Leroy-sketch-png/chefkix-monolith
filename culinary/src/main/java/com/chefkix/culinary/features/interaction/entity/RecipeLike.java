package com.chefkix.culinary.features.interaction.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.Instant;

@Document(collection = "recipe_likes")
// Tạo index kép để đảm bảo 1 User chỉ Like 1 Recipe 1 lần (Unique) và tìm kiếm siêu nhanh
@CompoundIndex(name = "recipe_user_idx", def = "{'recipeId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeLike {
    String id; // Mongo tự sinh ID string
    String recipeId;
    String userId;

    @CreatedDate
    Instant createdAt;
    // Có thể thêm createdAt nếu muốn sort theo thời gian like
}