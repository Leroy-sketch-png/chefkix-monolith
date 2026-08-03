package com.chefkix.culinary.features.session.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.session.dto.response.CurrentSessionResponse;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CookingSessionControllerTest {

    @Mock
    private CookingSessionService sessionService;

    @InjectMocks
    private CookingSessionController controller;

    @Test
    void getCurrentSessionReturnsSuccessfulAbsence() {
        when(sessionService.getCurrentSession()).thenReturn(null);

        ApiResponse<CurrentSessionResponse> response = controller.getCurrentSession();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getData()).isNull();
    }

    @Test
    void getCurrentSessionReturnsTheResumableSession() {
        CurrentSessionResponse current = CurrentSessionResponse.builder()
                .sessionId("session-1")
                .status(SessionStatus.PAUSED)
                .build();
        when(sessionService.getCurrentSession()).thenReturn(current);

        ApiResponse<CurrentSessionResponse> response = controller.getCurrentSession();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getData()).isSameAs(current);
    }
}
