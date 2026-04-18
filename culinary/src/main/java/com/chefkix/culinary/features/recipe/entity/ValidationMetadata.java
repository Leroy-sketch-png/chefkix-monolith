package com.chefkix.culinary.features.recipe.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationMetadata {
    boolean xpValidated;           // Has it been validated by AI?
    double validationConfidence;   // Confidence score (0.0 - 1.0)

    @Builder.Default
    List<String> validationIssues = new ArrayList<>(); // List of issues (e.g.: "Time too short")

    boolean xpAdjusted;            // Was XP deducted due to violations?
}