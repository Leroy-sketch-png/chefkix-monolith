package com.chefkix.culinary.features.session.service;

import com.chefkix.culinary.features.session.controller.SubstitutionFeedbackController.SubstitutionFeedbackRequest;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import com.chefkix.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubstitutionFeedbackServiceTest {

    @Mock CookingSessionRepository sessionRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks SubstitutionFeedbackService service;

    @Test
    void submitVerifiesOwnershipAndWaitsForBrokerAcknowledgement() {
        when(sessionRepository.findById("session-1"))
                .thenReturn(Optional.of(CookingSession.builder().id("session-1").userId("user-1")
                        .status(SessionStatus.POSTED).build()));
        when(kafkaTemplate.send(eq("substitution-feedback"), eq("user-1"), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        String eventId = service.submit("user-1", "session-1", request("client-1"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("substitution-feedback"), eq("user-1"), eventCaptor.capture());
        SubstitutionFeedbackEvent event = (SubstitutionFeedbackEvent) eventCaptor.getValue();
        assertEquals(event.getEventId(), eventId);
        assertEquals("client-1", event.getClientFeedbackId());
        assertEquals("signed-candidate-receipt", event.getCandidateReceipt());
        assertTrue(event.isSessionCompleted());
        assertTrue(event.isShared());
        assertTrue(eventId.matches("sub_feedback:v1:[0-9a-f]{64}"));
    }

    @Test
    void submitRejectsForeignSessionBeforePublishing() {
        when(sessionRepository.findById("session-1"))
                .thenReturn(Optional.of(CookingSession.builder().id("session-1").userId("other").build()));

        assertThrows(AppException.class, () -> service.submit("user-1", "session-1", request("client-1")));
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void submitDoesNotClaimSuccessWhenBrokerFails() {
        when(sessionRepository.findById("session-1"))
                .thenReturn(Optional.of(CookingSession.builder().id("session-1").userId("user-1").build()));
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(eq("substitution-feedback"), eq("user-1"), any()))
                .thenReturn(failed);

        assertThrows(AppException.class, () -> service.submit("user-1", "session-1", request("client-1")));
    }

    @Test
    void clientFeedbackIdMakesRetriesStableAndSeparateActionsDistinct() {
        when(sessionRepository.findById("session-1"))
                .thenReturn(Optional.of(CookingSession.builder().id("session-1").userId("user-1").build()));
        when(kafkaTemplate.send(eq("substitution-feedback"), eq("user-1"), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        String first = service.submit("user-1", "session-1", request("client-1"));
        String retry = service.submit("user-1", "session-1", request("client-1"));
        String secondAction = service.submit("user-1", "session-1", request("client-2"));

        assertEquals(first, retry);
        assertNotEquals(first, secondAction);
    }

    @Test
    void stableActionIdentityStillFingerprintsConflictingPayloads() {
        SubstitutionFeedbackEvent first = SubstitutionFeedbackEvent.builder()
                .userId("user-1").sessionId("session-1").clientFeedbackId("client-1")
                .originalIngredient("butter").substituteIngredient("olive oil")
                .accepted(true).build();
        SubstitutionFeedbackEvent conflict = SubstitutionFeedbackEvent.builder()
                .userId("user-1").sessionId("session-1").clientFeedbackId("client-1")
                .originalIngredient("butter").substituteIngredient("olive oil")
                .accepted(false).build();

        assertEquals(first.getEventId(), conflict.getEventId());
        assertNotEquals(first.payloadFingerprint(), conflict.payloadFingerprint());
    }

    private SubstitutionFeedbackRequest request(String clientId) {
        SubstitutionFeedbackRequest request = new SubstitutionFeedbackRequest();
        request.setClientFeedbackId(clientId);
        request.setOriginalIngredient("butter");
        request.setSubstituteIngredient("olive oil");
        request.setCandidateReceipt("signed-candidate-receipt");
        request.setAccepted(true);
        request.setUserRating(4.0);
        return request;
    }
}
