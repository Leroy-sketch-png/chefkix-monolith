package com.chefkix.culinary.features.achievement.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CriteriaType {
    COOK_CUISINE_COUNT("cook_cuisine_count"),
    USE_TECHNIQUE_COUNT("use_technique_count"),
    TOTAL_COOKS("total_cooks"),
    STREAK_DAYS("streak_days"),
    COOK_AFTER_MIDNIGHT("cook_after_midnight"),
    BEAT_ESTIMATED_TIME("beat_estimated_time"),
    RECIPES_PUBLISHED("recipes_published"),
    FOLLOWERS_COUNT("followers_count"),
    LIKES_RECEIVED("likes_received"),
    OTHERS_COOKED_YOUR_RECIPES("others_cooked_your_recipes");

    private final String value;

    CriteriaType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
