package com.chefkix.shared.exception;

import lombok.Getter;

/**
 * Application-level exception carrying an {@link ErrorCode}.
 * <p>
 * Unified from 5 service-specific copies. Supports four constructor shapes:
 * <ol>
 *   <li>ErrorCode only — uses the code's default message</li>
 *   <li>ErrorCode + custom message — overrides default message</li>
 *   <li>ErrorCode + cause — default message with root cause</li>
 *   <li>ErrorCode + custom message + cause — full override</li>
 * </ol>
 * Caught and translated to HTTP responses by {@link GlobalExceptionHandler}.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
