package com.chefkix.social.moderation.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

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
    String decision;

    String notes;

    /**
     * If decision is "ban_user", specify scope: "post", "comment", "all"
     */
    String banScope;
}
