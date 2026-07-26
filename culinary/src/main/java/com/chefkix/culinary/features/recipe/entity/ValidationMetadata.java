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
boolean xpValidated;
double validationConfidence;

    @Builder.Default
List<String> validationIssues = new ArrayList<>();

boolean xpAdjusted;
}