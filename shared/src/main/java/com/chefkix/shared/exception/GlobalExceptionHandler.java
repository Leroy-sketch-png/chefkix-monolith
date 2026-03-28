package com.chefkix.shared.exception;

import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for the entire monolith.
 * <p>
 * Unified from 5 service-specific copies. Handles:
 * <ul>
 *   <li>{@link AppException} — domain errors with {@link ErrorCode}</li>
 *   <li>{@link MethodArgumentNotValidException} — bean validation failures</li>
 *   <li>{@link AccessDeniedException} — Spring Security access denial</li>
 *   <li>{@link MaxUploadSizeExceededException} — file upload too large</li>
 *   <li>{@link RuntimeException} — catch-all for unhandled errors</li>
 * </ul>
 * Every error response sets {@code success=false} consistently.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    // ─── AppException (domain errors) ───────────────────────────────

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<?>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage();

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .statusCode(errorCode.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    // ─── Bean validation ────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String enumKey = ex.getFieldError() != null ? ex.getFieldError().getDefaultMessage() : null;
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        if (enumKey != null) {
            try {
                errorCode = ErrorCode.valueOf(enumKey);
                var constraintViolation =
                        ex.getBindingResult().getAllErrors().getFirst().unwrap(ConstraintViolation.class);
                attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            } catch (IllegalArgumentException ignored) {
                // enumKey doesn't match an ErrorCode — fall through to INVALID_KEY
            }
        }

        String message = Objects.nonNull(attributes)
                ? mapAttributes(errorCode.getMessage(), attributes)
                : errorCode.getMessage();

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .statusCode(errorCode.getCode())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // ─── Access denied ──────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // ─── Upload size exceeded ───────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<?>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload too large: {}", ex.getMessage());
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .statusCode(413)
                .message("File too large. Maximum upload size is 10MB per file.")
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // ─── Catch-all ──────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Invalid value for parameter '%s'. Expected type: %s", paramName, requiredType);
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .statusCode(400)
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .statusCode(400)
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException: {}", ex.getMessage(), ex);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .statusCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private String mapAttributes(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
