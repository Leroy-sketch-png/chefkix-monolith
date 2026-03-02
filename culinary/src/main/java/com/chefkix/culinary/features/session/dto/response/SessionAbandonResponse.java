package com.chefkix.culinary.features.session.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionAbandonResponse {
    private String sessionId;
    private String status; // "abandoned"
    private LocalDateTime abandonedAt;
    private boolean abandoned; // For FE compatibility
}
