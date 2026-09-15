package com.chefkix.culinary.features.session.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.session.service.SubstitutionFeedbackService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubstitutionFeedbackControllerTest {

    @Mock
    private SubstitutionFeedbackService feedbackService;

    @InjectMocks
    private SubstitutionFeedbackController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesValidatedEvidenceToTheDurableService() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null));
        var request = new SubstitutionFeedbackController.SubstitutionFeedbackRequest();
        request.setClientFeedbackId("client-1");
        request.setOriginalIngredient("butter");
        request.setSubstituteIngredient("coconut oil");
        request.setCandidateReceipt("receipt-v1");
        request.setAccepted(true);
        when(feedbackService.submit("user-1", "session-1", request)).thenReturn("event-1");

        var response = controller.submitFeedback("session-1", request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).contains("event-1");
        verify(feedbackService).submit("user-1", "session-1", request);
    }
}
