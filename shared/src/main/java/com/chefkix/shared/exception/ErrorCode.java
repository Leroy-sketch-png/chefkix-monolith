package com.chefkix.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Unified error codes for the entire ChefKix platform.
 * <p>
 * Consolidated from 5 service-specific copies. Legacy entries from the
 * book-sharing template (BOOK_*, CATEGORY_*, LISTING_*, AUTHOR_*) have been
 * removed. All codes use HTTP-style status numbers (4xx/5xx).
 * <p>
 * Grouped by domain for readability. New codes should be added to the
 * appropriate section and follow the naming convention: {@code ENTITY_ACTION}
 * or {@code ENTITY_STATE}.
 */
@Getter
public enum ErrorCode {

    // ─── GENERAL ────────────────────────────────────────────────────

    UNCATEGORIZED_EXCEPTION(500, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT(400, "Invalid input data", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(400, "Invalid request", HttpStatus.BAD_REQUEST),
    INVALID_KEY(400, "Invalid key", HttpStatus.BAD_REQUEST),
    DUPLICATE_KEY(409, "Duplicate key", HttpStatus.CONFLICT),
    UNAUTHENTICATED(401, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(403, "You do not have permission", HttpStatus.FORBIDDEN),

    // ─── FILE / IMAGE UPLOAD ────────────────────────────────────────

    FILE_UPLOAD_FAILED(400, "File upload failed", HttpStatus.BAD_REQUEST),
    FILE_DELETE_FAILED(400, "File delete failed", HttpStatus.BAD_REQUEST),
    CAN_NOT_UPLOAD_IMAGE(500, "Image upload failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // ─── IDENTITY / AUTH ────────────────────────────────────────────

    USER_EXISTED(409, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(409, "Email already exists", HttpStatus.CONFLICT),
    EMAIL_NOT_FOUND(404, "Email not found", HttpStatus.NOT_FOUND),
    USERNAME_NOT_FOUND(404, "Username not found", HttpStatus.NOT_FOUND),
    USERNAME_IS_MISSING(400, "Please enter username", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(400, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(400, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    TERMS_NOT_ACCEPTED(400, "Terms not accepted", HttpStatus.BAD_REQUEST),
    PROFILE_ALREADY_EXISTS(409, "Profile already exists", HttpStatus.CONFLICT),
    PROFILE_NOT_FOUND(404, "Profile not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(404, "Role not found", HttpStatus.NOT_FOUND),

    // ─── OTP ────────────────────────────────────────────────────────

    OTP_EXPIRED(400, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_INVALID(400, "Invalid OTP", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(404, "OTP not found", HttpStatus.NOT_FOUND),
    OTP_MAX_ATTEMPTS(429, "Too many OTP attempts", HttpStatus.TOO_MANY_REQUESTS),
    OTP_RESEND_TOO_FAST(429, "Please wait before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS),

    // ─── SOCIAL / FOLLOWING ─────────────────────────────────────────

    ALREADY_FRIEND(409, "Already following this user", HttpStatus.CONFLICT),
    NOT_FRIEND(400, "You are not following this user", HttpStatus.BAD_REQUEST),
    ALREADY_BLOCKED(409, "User already blocked", HttpStatus.CONFLICT),
    NOT_BLOCKED(400, "User is not blocked", HttpStatus.BAD_REQUEST),
    CANNOT_FOLLOW_SELF(400, "Cannot follow yourself", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_SELF(400, "Cannot block yourself", HttpStatus.BAD_REQUEST),

    // ─── RECIPE / COOKING SESSION ───────────────────────────────────

    RECIPE_NOT_FOUND(404, "Recipe not found", HttpStatus.NOT_FOUND),
    DRAFT_NOT_FOUND(404, "Draft not found", HttpStatus.NOT_FOUND),
    DRAFT_VALIDATION_FAILED(400, "Draft validation failed", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(400, "Validation error", HttpStatus.BAD_REQUEST),
    SESSION_NOT_FOUND(404, "Cooking session not found", HttpStatus.NOT_FOUND),
    SESSION_ALREADY_ACTIVE(409, "A cooking session is already active", HttpStatus.CONFLICT),
    SESSION_EXPIRED(410, "Session expired. Pause limit exceeded.", HttpStatus.GONE),
    SESSION_COMPLETED(409, "Session already completed", HttpStatus.CONFLICT),
    SESSION_ALREADY_LINKED(409, "Session already linked to a post", HttpStatus.CONFLICT),
    INVALID_TARGET_STEP(400, "Invalid target step", HttpStatus.BAD_REQUEST),
    INVALID_NAVIGATION_ACTION(400, "Invalid navigation action", HttpStatus.BAD_REQUEST),
    CANNOT_PAUSE_WITH_ACTIVE_TIMERS(400, "Cannot pause while timers are active", HttpStatus.BAD_REQUEST),
    COMPLETION_NOT_FOUND(404, "Completion not found", HttpStatus.NOT_FOUND),
    RATE_LIMIT_EXCEEDED(429, "Rate limit exceeded, please try again later", HttpStatus.TOO_MANY_REQUESTS),
    CHALLENGE_NOT_FOUND(404, "Challenge not found", HttpStatus.NOT_FOUND),

    // ─── POST / FEED ────────────────────────────────────────────────

    POST_NOT_FOUND(404, "Post not found", HttpStatus.NOT_FOUND),
    POST_EDIT_EXPIRED(400, "Post edit window expired", HttpStatus.BAD_REQUEST),
    POST_ACCESS_DENIED(403, "Not allowed to perform this action on this post", HttpStatus.FORBIDDEN),
    COMMENT_NOT_FOUND(404, "Comment not found", HttpStatus.NOT_FOUND),
    REPLY_NOT_FOUND(404, "Reply not found", HttpStatus.NOT_FOUND),
    REPORT_LIMIT_EXCEEDED(429, "Report limit exceeded for today", HttpStatus.TOO_MANY_REQUESTS),
    DUPLICATE_REPORT(409, "Already reported this content", HttpStatus.CONFLICT),

    // ─── CHAT ───────────────────────────────────────────────────────

    CONVERSATION_NOT_FOUND(404, "Conversation not found", HttpStatus.NOT_FOUND),
    INVALID_MESSAGE(400, "Invalid message", HttpStatus.BAD_REQUEST),

    // ─── NOTIFICATION ───────────────────────────────────────────────

    CANNOT_SEND_EMAIL(500, "Cannot send email", HttpStatus.INTERNAL_SERVER_ERROR),

    // ─── CROSS-MODULE COMMUNICATION ─────────────────────────────────

    POST_SERVICE_ERROR(500, "Post service communication error", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
