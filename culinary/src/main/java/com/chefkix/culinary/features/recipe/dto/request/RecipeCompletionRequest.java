package com.chefkix.culinary.features.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCompletionRequest {
    // Chỉ nhận bằng chứng từ Client
    List<String> proofImageUrls;

    @Valid
    @NotEmpty
    List<TimerLog> timerLogs;

    // Rating & Notes (Optional - cho Mastery)
    @Min(1) @Max(5)
    Integer rating;
    String notes;

    @Data
    public static class TimerLog {
        int stepNumber;
        long elapsedSeconds;
    }
}