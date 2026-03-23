package com.chefkix.culinary.features.achievement.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AchievementCategory {
    CUISINE("Cuisine"),
    TECHNIQUE("Technique"),
    SOCIAL("Social"),
    HIDDEN("Hidden"),
    SEASONAL("Seasonal");

    private final String value;

    AchievementCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
