package com.chefkix.culinary.features.shoppinglist.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingListItem {
    String itemId;
    String ingredient;
    String quantity;
    String unit;
    String category;
    @Builder.Default List<String> recipes = new ArrayList<>();
    boolean checked;
    boolean addedManually;
}
