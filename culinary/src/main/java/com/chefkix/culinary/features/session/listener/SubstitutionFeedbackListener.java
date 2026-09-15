package com.chefkix.culinary.features.session.listener;

import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubstitutionFeedbackListener {

    private static final String IDEMPOTENCY_SCOPE = "substitution-feedback:ai-projection";
    private final AIRestClient aiRestClient;
    private final KafkaIdempotencyService idempotencyService;

    @KafkaListener(
            topics = "substitution-feedback",
            groupId = "substitution-feedback-ai-projection-group",
            containerFactory = "substitutionFeedbackKafkaListenerContainerFactory")
    public void listen(SubstitutionFeedbackEvent event) {
        if (event == null || isBlank(event.getEventId()) || isBlank(event.getUserId())
                || isBlank(event.getSessionId()) || isBlank(event.getClientFeedbackId())
                || isBlank(event.getOriginalIngredient()) || isBlank(event.getSubstituteIngredient())
                || isBlank(event.getCandidateReceipt())) {
            log.error("Rejecting structurally invalid substitution feedback event");
            throw new IllegalArgumentException("Structurally invalid substitution feedback event");
        }
        String idempotencyIdentity = event.getEventId() + ":" + event.payloadFingerprint();
        if (!idempotencyService.tryProcess(idempotencyIdentity, IDEMPOTENCY_SCOPE)) {
            return;
        }
        try {
            aiRestClient.recordSubstitutionFeedback(event);
            log.info("Projected substitution feedback eventId={} to AI evidence store", event.getEventId());
        } catch (Exception e) {
            idempotencyService.removeProcessed(idempotencyIdentity, IDEMPOTENCY_SCOPE);
            throw e;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
