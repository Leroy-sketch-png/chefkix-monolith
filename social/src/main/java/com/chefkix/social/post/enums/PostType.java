package com.chefkix.social.post.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PostType {
    PERSONAL("PERSONAL"),
    GROUP("GROUP"),
    QUICK("QUICK"),
    POLL("POLL"),
    RECENT_COOK("RECENT_COOK"),
    RECIPE_REVIEW("RECIPE_REVIEW"),
    QUICK_TIP("QUICK_TIP"),
    RECIPE_BATTLE("RECIPE_BATTLE");

    private final String value;

    PostType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}