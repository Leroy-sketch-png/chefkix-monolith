package com.chefkix.culinary.features.session.listener;

import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubstitutionFeedbackListenerTest {

    private static final String SCOPE = "substitution-feedback:ai-projection";
    @Mock AIRestClient aiRestClient;
    @Mock KafkaIdempotencyService idempotencyService;
    @InjectMocks SubstitutionFeedbackListener listener;

    @Test
    void validEventIsProjectedOnce() {
        SubstitutionFeedbackEvent event = event();
        when(idempotencyService.tryProcess(identity(event), SCOPE)).thenReturn(true);

        listener.listen(event);

        verify(aiRestClient).recordSubstitutionFeedback(event);
    }

    @Test
    void duplicateEventIsNotProjected() {
        SubstitutionFeedbackEvent event = event();
        when(idempotencyService.tryProcess(identity(event), SCOPE)).thenReturn(false);

        listener.listen(event);

        verify(aiRestClient, never()).recordSubstitutionFeedback(event);
    }

    @Test
    void invalidEventNeverClaimsIdempotency() {
        SubstitutionFeedbackEvent event = event();
        event.setClientFeedbackId(" ");

        assertThrows(IllegalArgumentException.class, () -> listener.listen(event));

        verify(idempotencyService, never()).tryProcess(identity(event), SCOPE);
        verify(aiRestClient, never()).recordSubstitutionFeedback(event);
    }

    @Test
    void eventWithoutCandidateReceiptNeverReachesAiProjection() {
        SubstitutionFeedbackEvent event = event();
        event.setCandidateReceipt(" ");

        assertThrows(IllegalArgumentException.class, () -> listener.listen(event));

        verify(idempotencyService, never()).tryProcess(identity(event), SCOPE);
        verify(aiRestClient, never()).recordSubstitutionFeedback(event);
    }

    @Test
    void deliveryFailureReleasesIdempotencyForKafkaRetry() {
        SubstitutionFeedbackEvent event = event();
        when(idempotencyService.tryProcess(identity(event), SCOPE)).thenReturn(true);
        RuntimeException failure = new RuntimeException("AI unavailable");
        doThrow(failure).when(aiRestClient).recordSubstitutionFeedback(event);

        assertThrows(RuntimeException.class, () -> listener.listen(event));

        verify(idempotencyService).removeProcessed(identity(event), SCOPE);
    }

    private SubstitutionFeedbackEvent event() {
        return SubstitutionFeedbackEvent.builder()
                .userId("user-1")
                .sessionId("session-1")
                .clientFeedbackId("client-1")
                .originalIngredient("butter")
                .substituteIngredient("olive oil")
                .candidateReceipt("signed-candidate-receipt")
                .accepted(true)
                .sessionCompleted(true)
                .build();
    }

    private String identity(SubstitutionFeedbackEvent event) {
        return event.getEventId() + ":" + event.payloadFingerprint();
    }
}
