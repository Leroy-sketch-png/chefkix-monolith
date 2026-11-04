package com.chefkix.culinary.features.shoppinglist.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.CompoundIndex;

class ShoppingListIndexTest {

    @Test
    void cookPlanUniquenessIndexExcludesLegacyLists() {
        CompoundIndex cookPlanIndex = ShoppingList.class.getAnnotation(CompoundIndex.class);
        Document keys = Document.parse(cookPlanIndex.def());
        Document filter = Document.parse(cookPlanIndex.partialFilter());

        assertThat(cookPlanIndex.name()).isEqualTo("user_cook_plan_unique");
        assertThat(keys)
                .containsEntry("userId", 1)
                .containsEntry("sourceCookPlanId", 1);
        assertThat(cookPlanIndex.unique()).isTrue();
        assertThat(filter.get("sourceCookPlanId", Document.class).getString("$type"))
                .isEqualTo("string");
    }
}
