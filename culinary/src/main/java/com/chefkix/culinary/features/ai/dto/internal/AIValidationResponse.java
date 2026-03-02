package com.chefkix.culinary.features.ai.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIValidationResponse {
    boolean schemaValid;
    boolean contentSafe;
    List<String> issues;
    boolean isRealFood;
    double legitimacyScore;
    String aiAnalysis;
}