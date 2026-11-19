package com.chefkix.culinary.features.knowledge.dto;

import java.util.List;

public record PhotoSubstitutionResponse(
        String status,
        String dishName,
        String sourceIngredient,
        List<String> allergenConstraints,
        List<Candidate> candidates,
        int withheldCount,
        int blockedCount,
        String graphScope) {

    public record Candidate(String alternative, String context, Double ratio, boolean targetResolved) {
    }
}
