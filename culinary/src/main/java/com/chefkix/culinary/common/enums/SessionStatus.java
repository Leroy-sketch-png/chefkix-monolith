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
    IN_PROGRESS("in_progress"),   // Đang nấu
    PAUSED("paused"),             // Tạm dừng
    COMPLETED("completed"),       // Đã nấu xong (đã nhận 30% XP)
    POSTED("posted"),             // Đã đăng bài (đã nhận nốt 70% XP)
    ABANDONED("abandoned"),       // Bỏ cuộc / Timeout
    EXPIRED("expired");           // Quá hạn đăng bài (mất 70% XP)

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