package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AIValidationResponse {
    private boolean schemaValid;
    private boolean contentSafe;
    private List<String> issues;
    private boolean isRealFood;
    private boolean isSafeToConsume;
    private double legitimacyScore;
    private String aiAnalysis;
    private boolean aiUsed;
    private List<String> safetyIssues;
    private List<String> coherenceIssues;
    private List<String> dietaryConflicts;
    private List<String> suggestions;
}