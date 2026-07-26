package com.chefkix.social.post.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
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
int reportCount;
boolean reviewTriggered;
    Instant createdAt;
}
