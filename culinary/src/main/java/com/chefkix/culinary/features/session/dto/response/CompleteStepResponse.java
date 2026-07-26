package com.chefkix.culinary.features.session.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStepResponse {

    private String sessionId;
    
    private int completedStep;
    
    private List<Integer> completedSteps;
    
    private int totalSteps;
    
    private boolean allStepsComplete;
    
    private boolean alreadyCompleted;
}
