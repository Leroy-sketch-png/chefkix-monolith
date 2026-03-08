package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for POST /api/v1/moderate on the Python AI service.
 * Python response uses snake_case (no camelCase aliases for moderation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIModerationResponse {

    /** Moderation action: "approve", "flag", or "block" */
    private String action;

    /** Content category: "toxic", "spam", "off_topic", or "clean" */
    private String category;

    /** Severity level: "low", "medium", "high", or "critical" */
    private String severity;

    /** Confidence score (0.0–1.0) */
    private double confidence;

    /** Overall moderation score (0–100) */
    private int score;

    /** Human-readable explanation */
    private String reason;

    /** Matched blocked terms, if any */
    @JsonProperty("matched_terms")
    private List<String> matchedTerms;

    /** Whether AI was invoked for this decision */
    @JsonProperty("ai_used")
    private boolean aiUsed;

    // ---- Convenience methods ----

    /** Returns true if the content was approved (not flagged or blocked) */
    public boolean isApproved() {
        return "approve".equalsIgnoreCase(action);
    }

    /** Returns true if the content was blocked */
    public boolean isBlocked() {
        return "block".equalsIgnoreCase(action);
    }

    /** Returns true if severity is high or critical */
    public boolean isSevere() {
        return "high".equalsIgnoreCase(severity) || "critical".equalsIgnoreCase(severity);
    }
}
