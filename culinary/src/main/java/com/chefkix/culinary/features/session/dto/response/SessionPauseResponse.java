package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.culinary.common.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionPauseResponse {
    private String sessionId;
    private SessionStatus status;
    private LocalDateTime pauseAt;
    private LocalDateTime resumeDeadline;
}
