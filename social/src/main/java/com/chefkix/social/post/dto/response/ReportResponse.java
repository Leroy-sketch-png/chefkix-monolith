package com.chefkix.social.post.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response DTO for report operations.
 * Per spec 13-moderation.txt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportResponse {
    String reportId;
    String targetType;
    String targetId;
    String reason;
    int reportCount; // Total reports on this target
    boolean reviewTriggered; // True if threshold reached (3+ reports)
    Instant createdAt;
}
