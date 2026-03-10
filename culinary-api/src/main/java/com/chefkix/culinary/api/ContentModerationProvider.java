package com.chefkix.culinary.api;

/**
 * Cross-module contract for AI content moderation.
 * <p>
 * Implemented by {@code culinary} module (wraps AIRestClient),
 * consumed by {@code social} module for post/comment/chat moderation.
 */
public interface ContentModerationProvider {

    /**
     * Moderate user-generated content before saving.
     * <p>
     * Returns the moderation decision.
     * Implementations should be fail-open (allow content if AI service is down)
     * for non-critical content types like comments and chat.
     *
     * @param content     the text content to moderate
     * @param contentType the type: "post", "comment", "chat", "recipe"
     * @return moderation result
     */
    ModerationResult moderate(String content, String contentType);

    /**
     * Moderation result returned by the provider.
     */
    record ModerationResult(
            String action,      // "approve", "flag", "block"
            String category,    // "toxic", "spam", "off_topic", "clean"
            String severity,    // "low", "medium", "high", "critical"
            double confidence,
            String reason
    ) {
        public boolean isBlocked() {
            return "block".equals(action);
        }

        public boolean isApproved() {
            return "approve".equals(action);
        }

        /**
         * Default "approve" result for when AI service is unavailable.
         */
        public static ModerationResult approved() {
            return new ModerationResult("approve", "clean", "low", 1.0, "AI unavailable — auto-approved");
        }
    }
}
