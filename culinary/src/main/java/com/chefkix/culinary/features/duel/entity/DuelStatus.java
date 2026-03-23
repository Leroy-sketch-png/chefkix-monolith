package com.chefkix.culinary.features.duel.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle states for a 1-v-1 cooking duel.
 */
public enum DuelStatus {
    PENDING("Pending"),       // Challenger sent invite, waiting for response
    ACCEPTED("Accepted"),     // Opponent accepted, both can cook
    DECLINED("Declined"),     // Opponent declined
    IN_PROGRESS("In Progress"), // At least one session started
    COMPLETED("Completed"),   // Both cooked (or deadline passed), winner decided
    EXPIRED("Expired"),       // Nobody accepted within 48h
    CANCELLED("Cancelled");   // Challenger cancelled before acceptance

    private final String value;

    DuelStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
