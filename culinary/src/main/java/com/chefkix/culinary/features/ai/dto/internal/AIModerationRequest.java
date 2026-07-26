package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModerationRequest {

    private String content;

    @JsonProperty("content_type")
    @Builder.Default
    private String contentType = "recipe";

    @JsonProperty("user_reputation")
    @Builder.Default
    private int userReputation = 50;

    private String context;
}
