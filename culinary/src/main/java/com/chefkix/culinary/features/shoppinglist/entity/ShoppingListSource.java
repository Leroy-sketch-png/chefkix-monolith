package com.chefkix.culinary.features.shoppinglist.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ShoppingListSource {
    MEAL_PLAN("Meal Plan"),
    RECIPE("Recipe"),
    CUSTOM("Custom");

    private final String value;

    ShoppingListSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
