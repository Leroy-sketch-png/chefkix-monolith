package com.chefkix.culinary.features.session.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cooking-sessions")
@RequiredArgsConstructor
@Slf4j
public class SubstitutionFeedbackController {

    private static final String TOPIC = "substitution-feedback";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Data
    public static class SubstitutionFeedbackRequest {
        @NotBlank(message = "Original ingredient is required")
        private String originalIngredient;

        @NotBlank(message = "Substitute ingredient is required")
        private String substituteIngredient;

        private String technique;
        private String cuisine;
        private boolean accepted;
        private boolean sessionCompleted;
        private Double userRating;
        private boolean shared;
    }

    @PostMapping("/{sessionId}/substitution-feedback")
    public ApiResponse<String> submitFeedback(
            @PathVariable String sessionId,
            @Valid @RequestBody SubstitutionFeedbackRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        SubstitutionFeedbackEvent event = SubstitutionFeedbackEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .originalIngredient(request.getOriginalIngredient())
                .substituteIngredient(request.getSubstituteIngredient())
                .technique(request.getTechnique())
                .cuisine(request.getCuisine())
                .accepted(request.isAccepted())
                .sessionCompleted(request.isSessionCompleted())
                .userRating(request.getUserRating())
                .shared(request.isShared())
                .build();

        log.info("Publishing SubstitutionFeedbackEvent to topic {}: userId={}, sessionId={}, {} -> {}",
                TOPIC, userId, sessionId, request.getOriginalIngredient(), request.getSubstituteIngredient());

        kafkaTemplate.send(TOPIC, userId, event);

        return ApiResponse.success("Substitution feedback received and published to telemetry flywheel.");
    }
}
