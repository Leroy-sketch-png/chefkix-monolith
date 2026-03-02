package com.chefkix.culinary.features.recipe.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XpBreakDown {
    int base;
    String baseReason;
    int steps;
    String stepsReason;
    int time;
    String timeReason;
    Integer techniques;      // Optional
    String techniquesReason; // Optional
    int total;
}