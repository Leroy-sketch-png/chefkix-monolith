package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chefkix.identity.repository.CreatorTipSettingsRepository;
import com.chefkix.identity.repository.TipRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TipServiceTest {

    @Mock
    private CreatorTipSettingsRepository tipSettingsRepository;

    @Mock
    private TipRepository tipRepository;

    @InjectMocks
    private TipService tipService;

    @Test
    void sendTipFailsBeforeSettingsLookupOrPersistenceWithoutPaymentProcessing() {
        assertThatThrownBy(
                        () ->
                                tipService.sendTip(
                                        "tipper-1", "creator-1", "recipe-1", 500, "Thank you"))
                .isInstanceOfSatisfying(
                        AppException.class,
                        exception -> {
                            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                            assertThat(exception.getMessage())
                                    .isEqualTo(
                                            "Creator tipping is unavailable until payment processing is connected");
                        });

        verifyNoInteractions(tipSettingsRepository, tipRepository);
    }
}
