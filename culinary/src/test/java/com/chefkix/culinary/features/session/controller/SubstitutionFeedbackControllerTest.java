package com.chefkix.culinary.features.session.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.chefkix.shared.event.SubstitutionFeedbackChoice;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubstitutionFeedbackControllerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SubstitutionFeedbackController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesAnAcceptedSubstitutionWithItsExplicitChoice() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null));

        SubstitutionFeedbackController.SubstitutionFeedbackRequest request =
                new SubstitutionFeedbackController.SubstitutionFeedbackRequest();
        request.setOriginalIngredient("butter");
        request.setSubstituteIngredient("coconut oil");
        request.setChoice(SubstitutionFeedbackChoice.ACCEPT);
        request.setSessionCompleted(false);

        var response = controller.submitFeedback("session-1", request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().isRecorded()).isTrue();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("substitution-feedback"), eq("user-1"), eventCaptor.capture());

        SubstitutionFeedbackEvent event = (SubstitutionFeedbackEvent) eventCaptor.getValue();
        assertThat(event.getChoice()).isEqualTo(SubstitutionFeedbackChoice.ACCEPT);
        assertThat(event.isAccepted()).isTrue();
        assertThat(event.getEventId()).contains("coconut oil");
    }

    @Test
    void allowsSkipWithoutInventingASubstituteIngredient() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null));

        SubstitutionFeedbackController.SubstitutionFeedbackRequest request =
                new SubstitutionFeedbackController.SubstitutionFeedbackRequest();
        request.setOriginalIngredient("butter");
        request.setChoice(SubstitutionFeedbackChoice.SKIP);

        var response = controller.submitFeedback("session-1", request);

        assertThat(response.isSuccess()).isTrue();
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("substitution-feedback"), eq("user-1"), eventCaptor.capture());
        assertThat(((SubstitutionFeedbackEvent) eventCaptor.getValue()).getSubstituteIngredient())
                .isNull();
    }
}
