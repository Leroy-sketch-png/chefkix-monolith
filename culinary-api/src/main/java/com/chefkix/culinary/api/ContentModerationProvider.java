package com.chefkix.culinary.api;

/**
 */
public interface ContentModerationProvider {

    /**
     *
     */
    ModerationResult moderate(String content, String contentType);

    /**
     */
    record ModerationResult(
String action,
String category,
String severity,
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
         */
        public static ModerationResult approved() {
            return new ModerationResult("approve", "clean", "low", 1.0, "AI unavailable — auto-approved");
        }
    }
}
