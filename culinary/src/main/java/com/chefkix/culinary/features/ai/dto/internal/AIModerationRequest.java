package com.chefkix.culinary.features.ai.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for POST /api/v1/moderate on the Python AI service.
 * Used for server-side content moderation during recipe publish.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModerationRequest {

    /** The text content to moderate (title + description + steps concatenated) */
    private String content;

    /** Content type hint: "recipe", "comment", or "review" */
    @JsonProperty("content_type")
    @Builder.Default
    private String contentType = "recipe";

    /** User reputation score (0–100), affects moderation threshold */
    @JsonProperty("user_reputation")
    @Builder.Default
    private int userReputation = 50;

    /** Additional context for the AI moderator */
    private String context;
}
