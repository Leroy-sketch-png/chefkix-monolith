package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIModerationResponse {

    private String action;

    private String category;

    private String severity;

    private double confidence;

    private int score;

    private String reason;

    @JsonProperty("matched_terms")
    private List<String> matchedTerms;

    @JsonProperty("ai_used")
    private boolean aiUsed;


    public boolean isApproved() {
        return "approve".equalsIgnoreCase(action);
    }

    public boolean isBlocked() {
        return "block".equalsIgnoreCase(action);
    }

    public boolean isSevere() {
        return "high".equalsIgnoreCase(severity) || "critical".equalsIgnoreCase(severity);
    }
}
