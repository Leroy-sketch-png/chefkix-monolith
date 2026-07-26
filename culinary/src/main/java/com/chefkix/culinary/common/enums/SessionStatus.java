package com.chefkix.culinary.common.enums;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 */
public enum SessionStatus {
IN_PROGRESS("in_progress"),
PAUSED("paused"),
COMPLETED("completed"),
POSTED("posted"),
POST_DELETED("post_deleted"),
ABANDONED("abandoned"),
EXPIRED("expired");

    private final String value;

    SessionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean hasClaimedPostXp() {
        return this == POSTED || this == POST_DELETED;
    }

    public boolean countsAsCompletedCook() {
        return this == COMPLETED || hasClaimedPostXp();
    }

    @JsonCreator
    public static SessionStatus fromValue(String value) {
        for (SessionStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new AppException(ErrorCode.INVALID_INPUT, "Unknown session status: " + value);
    }
}