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
    USER_NOT_EXISTED(404, "User not exists", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(409, "Email already exists", HttpStatus.CONFLICT),
    EMAIL_NOT_FOUND(404, "Email not found", HttpStatus.NOT_FOUND),
    INVALID_EMAIL(400, "Invalid email", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_FOUND(404, "Username not found", HttpStatus.NOT_FOUND),
    USERNAME_IS_MISSING(400, "Please enter username", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(400, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(400, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    TERMS_NOT_ACCEPTED(400, "Terms not accepted", HttpStatus.BAD_REQUEST),
    PROFILE_ALREADY_EXISTS(409, "Profile already exists", HttpStatus.CONFLICT),
    PROFILE_NOT_FOUND(404, "Profile not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(404, "Role not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(401, "Invalid email/username or password. Please try again.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(403, "This account has been disabled. Please contact support.", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED(403, "Please verify your email before signing in.", HttpStatus.FORBIDDEN),
    SIGNUP_REQUEST_NOT_FOUND(404, "No pending registration found. Please sign up again.", HttpStatus.NOT_FOUND),
    RESET_PASSWORD_REQUEST_NOT_FOUND(404, "No pending password reset request. Please request again.", HttpStatus.NOT_FOUND),

    // ─── OTP ────────────────────────────────────────────────────────

    OTP_EXPIRED(400, "OTP has expired. Please request a new code.", HttpStatus.BAD_REQUEST),
    OTP_INVALID(400, "Invalid OTP. Please check the code and try again.", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(404, "OTP not found", HttpStatus.NOT_FOUND),
    OTP_MAX_ATTEMPTS(429, "Too many OTP attempts", HttpStatus.TOO_MANY_REQUESTS),
    OTP_MAX_ATTEMPTS_EXCEEDED(429, "Too many failed OTP attempts. Please sign up again.", HttpStatus.TOO_MANY_REQUESTS),
    OTP_RATE_LIMIT(400, "Please wait 60 seconds before re-sending a new OTP code", HttpStatus.BAD_REQUEST),
    OTP_HOURLY_LIMIT_EXCEEDED(400, "Too many OTP requests this hour. Please try again later.", HttpStatus.BAD_REQUEST),
    OTP_DAILY_LIMIT_EXCEEDED(400, "Too many OTP requests today. Please try again tomorrow.", HttpStatus.BAD_REQUEST),
    OTP_RESEND_TOO_FAST(429, "Please wait before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS),

    // ─── SOCIAL / FOLLOWING ─────────────────────────────────────────

    ALREADY_FRIEND(409, "Already following this user", HttpStatus.CONFLICT),
    NOT_FRIEND(400, "You are not following this user", HttpStatus.BAD_REQUEST),
    ALREADY_BLOCKED(409, "User already blocked", HttpStatus.CONFLICT),
    NOT_BLOCKED(400, "User is not blocked", HttpStatus.BAD_REQUEST),
    CANNOT_FOLLOW_SELF(400, "Cannot follow yourself", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_SELF(400, "Cannot block yourself", HttpStatus.BAD_REQUEST),
    BLOCK_NOT_FOUND(404, "Block not found", HttpStatus.NOT_FOUND),
    REQUEST_NOT_FOUND(404, "Request not found", HttpStatus.NOT_FOUND),
    DO_NOT_HAVE_PERMISSION(403, "You do not have this permission", HttpStatus.FORBIDDEN),
    INVALID_OPERATION(400, "Invalid operation", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS_FROM_IP(429, "Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),

    // ─── RECIPE / COOKING SESSION ───────────────────────────────────

    RECIPE_NOT_FOUND(404, "Recipe not found", HttpStatus.NOT_FOUND),
    DRAFT_NOT_FOUND(404, "Draft not found", HttpStatus.NOT_FOUND),
    DRAFT_VALIDATION_FAILED(400, "Draft validation failed", HttpStatus.BAD_REQUEST),
    RECIPE_VALIDATION_FAILED(400, "Recipe failed safety validation and cannot be published", HttpStatus.BAD_REQUEST),
    RECIPE_MODERATION_FAILED(400, "Recipe content was flagged by moderation and cannot be published", HttpStatus.BAD_REQUEST),
    AI_SERVICE_UNAVAILABLE(503, "AI service is temporarily unavailable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE),
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
    INVALID_ACTION(400, "Invalid action", HttpStatus.BAD_REQUEST),
    EMPTY(404, "No data found", HttpStatus.NOT_FOUND),
    NO_FRIEND_YET(404, "You have no friends yet", HttpStatus.NOT_FOUND),

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
    MESSAGE_NOT_FOUND(404, "Message not found", HttpStatus.NOT_FOUND),
    INVALID_MESSAGE(400, "Invalid message", HttpStatus.BAD_REQUEST),

    // ─── COOKING ROOM ───────────────────────────────────────────────

    ROOM_NOT_FOUND(404, "Cooking room not found or expired", HttpStatus.NOT_FOUND),
    ROOM_FULL(409, "Cooking room is full", HttpStatus.CONFLICT),
    ALREADY_IN_ROOM(409, "Already in this cooking room", HttpStatus.CONFLICT),
    NOT_IN_ROOM(400, "You are not in this cooking room", HttpStatus.BAD_REQUEST),

    // ─── NOTIFICATION ───────────────────────────────────────────────

    CANNOT_SEND_EMAIL(500, "Cannot send email", HttpStatus.INTERNAL_SERVER_ERROR),

    // ─── CROSS-MODULE COMMUNICATION ─────────────────────────────────

    POST_SERVICE_ERROR(500, "Post service communication error", HttpStatus.INTERNAL_SERVER_ERROR),

    // ─── PANTRY / MEAL PLAN / SHOPPING ──────────────────────────────

    PANTRY_ITEM_NOT_FOUND(404, "Pantry item not found", HttpStatus.NOT_FOUND),
    MEAL_PLAN_NOT_FOUND(404, "Meal plan not found", HttpStatus.NOT_FOUND),
    SHOPPING_LIST_NOT_FOUND(404, "Shopping list not found", HttpStatus.NOT_FOUND),

    // ─── ADMIN / MODERATION / BAN ──────────────────────────────────

    ADMIN_ACCESS_DENIED(403, "Admin access required", HttpStatus.FORBIDDEN),
    REPORT_NOT_FOUND(404, "Report not found", HttpStatus.NOT_FOUND),
    USER_BANNED(403, "Your account is currently suspended", HttpStatus.FORBIDDEN),
    BAN_NOT_FOUND(404, "Ban record not found", HttpStatus.NOT_FOUND),
    APPEAL_NOT_FOUND(404, "Appeal not found", HttpStatus.NOT_FOUND),
    APPEAL_ALREADY_EXISTS(409, "An active appeal already exists for this ban", HttpStatus.CONFLICT),
    CONTENT_MODERATION_FAILED(400, "Content was flagged by moderation and cannot be posted", HttpStatus.BAD_REQUEST),
    GROUP_NOT_FOUND(404, "Group not found", HttpStatus.NOT_FOUND),
    GROUP_MEMBER_NOT_FOUND(404, "Group member not found", HttpStatus.NOT_FOUND),
    GROUP_BANNED(409, "You are restricted from joining this group.", HttpStatus.CONFLICT),
    GROUP_ALREADY_IN(409, "You are already in this group.", HttpStatus.CONFLICT),
    PENDING_NOT_FOUND(404, "User is not in PENDING status", HttpStatus.NOT_FOUND),

    // ─── ACHIEVEMENTS / SKILL TREE ─────────────────────────────────

    ACHIEVEMENT_NOT_FOUND(404, "Achievement not found", HttpStatus.NOT_FOUND),
    ACHIEVEMENT_ALREADY_UNLOCKED(409, "Achievement already unlocked", HttpStatus.CONFLICT),
    ACHIEVEMENT_PREREQUISITE_NOT_MET(400, "Prerequisite achievement not yet unlocked", HttpStatus.BAD_REQUEST),

    // ─── COOKING DUELS ─────────────────────────────────

    DUEL_NOT_FOUND(404, "Duel not found", HttpStatus.NOT_FOUND),
    DUEL_ALREADY_EXISTS(409, "An active duel already exists between these users for this recipe", HttpStatus.CONFLICT),
    DUEL_CANNOT_CHALLENGE_SELF(400, "Cannot challenge yourself", HttpStatus.BAD_REQUEST),
    DUEL_NOT_PENDING(400, "Duel is not in pending state", HttpStatus.BAD_REQUEST),
    DUEL_NOT_PARTICIPANT(403, "You are not a participant in this duel", HttpStatus.FORBIDDEN),
    DUEL_ALREADY_HAS_SESSION(409, "You already have a cooking session for this duel", HttpStatus.CONFLICT),
    DUEL_NOT_ACCEPTED(400, "Duel has not been accepted yet", HttpStatus.BAD_REQUEST),

    // ─── REFERRAL ───────────────────────────────────────────────────

    REFERRAL_CODE_NOT_FOUND(404, "Referral code not found", HttpStatus.NOT_FOUND),
    REFERRAL_SELF_REDEEM(400, "Cannot redeem your own referral code", HttpStatus.BAD_REQUEST),
    REFERRAL_ALREADY_REDEEMED(409, "You have already redeemed a referral code", HttpStatus.CONFLICT),
    REFERRAL_CODE_EXHAUSTED(409, "This referral code has reached its maximum uses", HttpStatus.CONFLICT),

    // ─── SUBSCRIPTION / PREMIUM ─────────────────────────────────────

    SUBSCRIPTION_NOT_FOUND(404, "Subscription not found", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_ALREADY_ACTIVE(409, "An active subscription already exists", HttpStatus.CONFLICT),
    SUBSCRIPTION_EXPIRED(410, "Subscription has expired", HttpStatus.GONE),
    PREMIUM_FEATURE_REQUIRED(403, "This feature requires a premium subscription", HttpStatus.FORBIDDEN),

    // ─── VERIFICATION ───────────────────────────────────────────────

    VERIFICATION_REQUEST_NOT_FOUND(404, "Verification request not found", HttpStatus.NOT_FOUND),
    VERIFICATION_ALREADY_PENDING(409, "A verification request is already pending", HttpStatus.CONFLICT),
    VERIFICATION_ALREADY_VERIFIED(409, "Account is already verified", HttpStatus.CONFLICT),
    VERIFICATION_REQUIREMENTS_NOT_MET(400, "Verification requirements not met", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
