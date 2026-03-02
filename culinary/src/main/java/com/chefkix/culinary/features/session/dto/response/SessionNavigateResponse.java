package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.culinary.features.session.entity.CookingSession;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionNavigateResponse {
    private String sessionId;
    private int currentStep;
    private int previousStep;
    private List<CookingSession.ActiveTimer> activeTimers;
}
