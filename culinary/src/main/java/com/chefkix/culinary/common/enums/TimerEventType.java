package com.chefkix.culinary.common.enums;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Timer event types during a cooking session.
 * Uses lowercase for JSON serialization (FE-friendly).
 */
public enum TimerEventType {
    START("start"),
    COMPLETE("complete"),
    SKIP("skip");

    private final String value;

    TimerEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimerEventType fromValue(String value) {
        for (TimerEventType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new AppException(ErrorCode.INVALID_INPUT, "Unknown timer event type: " + value);
    }
}