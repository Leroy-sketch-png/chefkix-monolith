package com.chefkix.culinary.features.shoppinglist.dto.response;

import com.chefkix.culinary.features.shoppinglist.entity.ShoppingListItem;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingListResponse {
    String id;
    String name;
    List<ShoppingListItem> items;
    String source;
    String sourceMealPlanId;
    String sourceRecipeId;
    String shareToken;
    int totalItems;
    int checkedItems;
    Instant createdAt;
    Instant updatedAt;
}
