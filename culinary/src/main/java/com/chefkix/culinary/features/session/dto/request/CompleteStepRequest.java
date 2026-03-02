package com.chefkix.culinary.features.session.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to mark a step as completed.
 * Navigation and completion are separate actions:
 * - Navigate = move cursor (currentStep)
 * - CompleteStep = mark step done (add to completedSteps[])
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStepRequest {

    @NotNull(message = "Step number is required")
    @Min(value = 1, message = "Step number must be at least 1")
    private Integer stepNumber;
}
