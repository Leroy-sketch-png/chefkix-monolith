package com.chefkix.culinary.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Recipe Quality Score (RQS) tiers.
 * Computed by AI service, stored on Recipe entity.
 *
 * 85-100: FOOLPROOF — Exceptionally detailed, beginner-safe
 * 65-84:  GOOD — Solid recipe, minor gaps
 * 40-64:  NEEDS_WORK — Missing key details
 * 0-39:   DRAFT_QUALITY — Incomplete, not publishable
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
