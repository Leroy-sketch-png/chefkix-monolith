package com.chefkix.culinary.features.session.controller;

import com.chefkix.culinary.features.session.service.SubstitutionFeedbackService;
import com.chefkix.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cooking-sessions")
@RequiredArgsConstructor
public class SubstitutionFeedbackController {

    private final SubstitutionFeedbackService feedbackService;

    @Data
    public static class SubstitutionFeedbackRequest {
        @NotBlank(message = "Client feedback ID is required")
        @Size(max = 100)
        private String clientFeedbackId;

        @NotBlank(message = "Original ingredient is required")
        @Size(max = 200)
        private String originalIngredient;

        @NotBlank(message = "Substitute ingredient is required")
        @Size(max = 200)
        private String substituteIngredient;

        @NotBlank(message = "Candidate receipt is required")
        @Size(max = 2048)
        private String candidateReceipt;

        @Size(max = 100)
        private String technique;

        @Size(max = 100)
        private String cuisine;

        private boolean accepted;

        @DecimalMin("1.0")
        @DecimalMax("5.0")
        private Double userRating;
    }

    @PostMapping("/{sessionId}/substitution-feedback")
    public ApiResponse<String> submitFeedback(
            @PathVariable String sessionId,
            @Valid @RequestBody SubstitutionFeedbackRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String eventId = feedbackService.submit(userId, sessionId, request);
        return ApiResponse.success("Substitution feedback durably accepted as " + eventId);
    }
}
