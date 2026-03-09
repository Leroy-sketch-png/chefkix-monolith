package com.chefkix.culinary.features.shoppinglist.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingListSummaryResponse {
    String id;
    String name;
    String source;
    int totalItems;
    int checkedItems;
    Instant createdAt;
}
