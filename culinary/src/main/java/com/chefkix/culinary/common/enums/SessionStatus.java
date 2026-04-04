package com.chefkix.culinary.common.enums;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Cooking session lifecycle status.
 * Uses snake_case for JSON serialization (FE-friendly).
 */
public enum SessionStatus {
    IN_PROGRESS("in_progress"),   // Currently cooking
    PAUSED("paused"),             // Paused
    COMPLETED("completed"),       // Cooking finished (received 30% XP)
    POSTED("posted"),             // Post created (received remaining 70% XP)
    ABANDONED("abandoned"),       // Gave up / Timed out
    EXPIRED("expired");           // Post deadline passed (lost 70% XP)

    private final String value;

    SessionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
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