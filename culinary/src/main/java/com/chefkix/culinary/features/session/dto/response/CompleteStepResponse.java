package com.chefkix.culinary.features.session.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response after marking a step as completed.
 * Returns the updated completedSteps array and progress info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStepResponse {

    private String sessionId;
    
    /** The step that was just marked complete */
    private int completedStep;
    
    /** Full list of completed steps (may be out of order) */
    private List<Integer> completedSteps;
    
    /** Total steps in the recipe */
    private int totalSteps;
    
    /** True if all steps are now complete */
    private boolean allStepsComplete;
    
    /** Was this step already marked complete? (idempotent) */
    private boolean alreadyCompleted;
}
