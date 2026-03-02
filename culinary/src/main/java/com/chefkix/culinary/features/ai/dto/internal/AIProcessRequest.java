package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIProcessRequest {
    @JsonProperty("raw_text")
    private String rawText;

    @JsonProperty("user_id")
    private String userId;
}