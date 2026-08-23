package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Explicit user action captured by the substitution feedback instrument. */
public enum SubstitutionFeedbackChoice {
    ACCEPT,
    REJECT,
    SKIP;

    @JsonCreator
    public static SubstitutionFeedbackChoice fromValue(String value) {
        return value == null ? null : valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
