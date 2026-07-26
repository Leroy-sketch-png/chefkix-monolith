package com.chefkix.culinary.features.duel.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 */
public enum DuelStatus {
PENDING("Pending"),
ACCEPTED("Accepted"),
DECLINED("Declined"),
IN_PROGRESS("In Progress"),
COMPLETED("Completed"),
EXPIRED("Expired"),
CANCELLED("Cancelled");

    private final String value;

    DuelStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
