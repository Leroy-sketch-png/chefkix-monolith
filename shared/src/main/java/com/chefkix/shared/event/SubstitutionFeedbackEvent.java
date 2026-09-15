package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Event emitted when a user interacts with a substitution during or after a cooking session.
 * Carries raw evidence to an idempotent projection. It does not authorize a ranking update.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("SUBSTITUTION_FEEDBACK_ACTION")
public class SubstitutionFeedbackEvent extends BaseEvent {

    private String sessionId;
    private String clientFeedbackId;
    private String originalIngredient;
    private String substituteIngredient;
    private String candidateReceipt;
    private String technique;
    private String cuisine;
    private boolean accepted;
    private boolean sessionCompleted;
    private Double userRating; // 1.0 - 5.0 scale, nullable
    private boolean shared;

    @Builder
    public SubstitutionFeedbackEvent(String userId, String sessionId, String clientFeedbackId,
                                     String originalIngredient,
                                     String substituteIngredient, String candidateReceipt,
                                     String technique, String cuisine,
                                     boolean accepted, boolean sessionCompleted, Double userRating,
                                     boolean shared) {
        super("SUBSTITUTION_FEEDBACK_ACTION", userId);
        this.sessionId = sessionId;
        this.clientFeedbackId = clientFeedbackId;
        this.originalIngredient = originalIngredient;
        this.substituteIngredient = substituteIngredient;
        this.candidateReceipt = candidateReceipt;
        this.technique = technique;
        this.cuisine = cuisine;
        this.accepted = accepted;
        this.sessionCompleted = sessionCompleted;
        this.userRating = userRating;
        this.shared = shared;
        this.eventId = buildDeterministicEventId(userId, sessionId, clientFeedbackId);
    }

    private static String buildDeterministicEventId(String userId, String sessionId,
                                                     String clientFeedbackId) {
        if (userId == null || userId.isBlank() || sessionId == null || sessionId.isBlank()
                || clientFeedbackId == null || clientFeedbackId.isBlank()) {
            throw new IllegalArgumentException("Feedback event identity requires user, session, and client ID");
        }
        String canonical = "v1\u001f" + userId + "\u001f" + sessionId + "\u001f" + clientFeedbackId;
        return "sub_feedback:v1:" + sha256(canonical);
    }

    /**
     * Binds the idempotency mark to the exact payload so a reused client action ID with
     * different evidence reaches the durable projection and is rejected as a conflict.
     */
    public String payloadFingerprint() {
        String canonical = lengthPrefix(originalIngredient)
                + lengthPrefix(substituteIngredient)
                + lengthPrefix(candidateReceipt)
                + lengthPrefix(technique)
                + lengthPrefix(cuisine)
                + lengthPrefix(Boolean.toString(accepted))
                + lengthPrefix(Boolean.toString(sessionCompleted))
                + lengthPrefix(userRating == null ? null : userRating.toString())
                + lengthPrefix(Boolean.toString(shared));
        return sha256(canonical);
    }

    private static String lengthPrefix(String value) {
        return value == null ? "-1:" : value.length() + ":" + value;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
