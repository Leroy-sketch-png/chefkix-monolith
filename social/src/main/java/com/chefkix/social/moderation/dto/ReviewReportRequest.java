package com.chefkix.social.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Admin review request for a report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewReportRequest {
    /**
     * Decision: "resolved", "dismissed", "ban_user"
     */
    @NotBlank(message = "decision is required")
    @Pattern(
            regexp = "resolved|dismissed|ban_user",
            message = "decision must be one of: resolved, dismissed, ban_user")
    String decision;

    @Size(max = 1000, message = "notes must be at most 1000 characters")
    String notes;

    /**
     * If decision is "ban_user", specify scope: "post", "comment", "all"
     */
    @Pattern(regexp = "post|comment|all", message = "banScope must be one of: post, comment, all")
    String banScope;
}
