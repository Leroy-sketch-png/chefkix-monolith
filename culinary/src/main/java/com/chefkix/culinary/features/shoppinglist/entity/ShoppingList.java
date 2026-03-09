package com.chefkix.culinary.features.shoppinglist.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "shopping_lists")
public class ShoppingList {
    @Id String id;
    @Indexed String userId;
    String name;
    @Builder.Default List<ShoppingListItem> items = new ArrayList<>();
    ShoppingListSource source;
    String sourceMealPlanId;
    String sourceRecipeId;
    @Indexed(unique = true, sparse = true) String shareToken;
    @CreatedDate Instant createdAt;
    @LastModifiedDate Instant updatedAt;
}
