package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for creating a report.
 * Per spec 13-moderation.txt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportRequest {

    @NotBlank(message = "Target type is required")
    @Pattern(regexp = "^(post|comment|recipe)$", message = "Target type must be 'post', 'comment', or 'recipe'")
    String targetType;

    @NotBlank(message = "Target ID is required")
    String targetId;

    @NotBlank(message = "Reason is required")
    @Pattern(regexp = "^(fraud|spam|inappropriate|harassment|copyright|other)$", message = "Reason must be 'fraud', 'spam', 'inappropriate', 'harassment', 'copyright', or 'other'")
    String reason;

    String details; // Optional explanation
}
