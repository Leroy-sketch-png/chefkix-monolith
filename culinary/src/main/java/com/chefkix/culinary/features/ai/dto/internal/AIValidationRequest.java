package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIValidationRequest {
    String title;
    String description;
    List<String> ingredients;
    List<String> steps;

    @JsonProperty("check_safety")
    @Builder.Default
    boolean checkSafety = true;
}