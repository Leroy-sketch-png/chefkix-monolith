package com.chefkix.culinary.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum QualityTier {
    FOOLPROOF("Foolproof"),
    GOOD("Good"),
    NEEDS_WORK("Needs Work"),
    DRAFT_QUALITY("Draft Quality");

    private final String value;

    QualityTier(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static QualityTier fromValue(String value) {
        if (value == null) return null;
        for (QualityTier tier : QualityTier.values()) {
            if (tier.value.equalsIgnoreCase(value) || tier.name().equalsIgnoreCase(value)) {
                return tier;
            }
        }
        return null;
    }
}
