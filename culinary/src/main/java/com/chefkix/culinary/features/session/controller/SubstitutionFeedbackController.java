package com.chefkix.culinary.features.session.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.event.SubstitutionFeedbackChoice;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

        private String substituteIngredient;

        private String technique;
        private String cuisine;
        @NotNull(message = "Substitution choice is required")
        private SubstitutionFeedbackChoice choice;

        private boolean sessionCompleted;

        @Min(value = 1, message = "userRating must be at least 1")
        @Max(value = 5, message = "userRating must be at most 5")
        private Double userRating;

        @Pattern(regexp = "up|neutral|down", message = "tasteFeedback must be up, neutral, or down")
        private String tasteFeedback;

        private boolean shared;

        @AssertTrue(message = "A substitute ingredient is required for accept or reject")
        public boolean hasSubstituteWhenNeeded() {
            return choice == SubstitutionFeedbackChoice.SKIP
                    || (substituteIngredient != null && !substituteIngredient.isBlank());
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class FeedbackReceipt {
        private final boolean recorded;
        private final String eventId;
    }

    @PostMapping("/{sessionId}/substitution-feedback")
    public ApiResponse<FeedbackReceipt> submitFeedback(
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
                .choice(request.getChoice())
                .accepted(request.getChoice() == SubstitutionFeedbackChoice.ACCEPT)
                .sessionCompleted(request.isSessionCompleted())
                .userRating(request.getUserRating())
                .tasteFeedback(request.getTasteFeedback())
                .shared(request.isShared())
                .build();

        log.info("Publishing SubstitutionFeedbackEvent to topic {}: userId={}, sessionId={}, {} -> {}",
                TOPIC, userId, sessionId, request.getOriginalIngredient(), request.getSubstituteIngredient());

        kafkaTemplate.send(TOPIC, userId, event);

        return ApiResponse.success(new FeedbackReceipt(true, event.getEventId()));
    }
}
