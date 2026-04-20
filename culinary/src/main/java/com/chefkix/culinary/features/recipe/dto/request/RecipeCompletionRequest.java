package com.chefkix.culinary.features.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCompletionRequest {
    // Only accept proof from Client
    @Size(max = 10)
    List<@Size(max = 500) String> proofImageUrls;

    @Valid
    @NotEmpty
    List<TimerLog> timerLogs;

    // Rating & Notes (Optional - for Mastery)
    @Min(1) @Max(5)
    Integer rating;
    @Size(max = 2000)
    String notes;

    @Data
    public static class TimerLog {
        int stepNumber;
        long elapsedSeconds;
    }
}