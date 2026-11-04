package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionProviderImplTest {

    private static final String USER_ID = "provider-tester";

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionProviderImpl subscriptionProvider;

    @Test
    void isPremiumDelegatesToSubscriptionService() {
        when(subscriptionService.isPremium(USER_ID)).thenReturn(true);
        assertThat(subscriptionProvider.isPremium(USER_ID)).isTrue();
        verify(subscriptionService).isPremium(USER_ID);

        when(subscriptionService.isPremium(USER_ID)).thenReturn(false);
        assertThat(subscriptionProvider.isPremium(USER_ID)).isFalse();
    }

    @Test
    void requirePremiumDelegatesToSubscriptionService() {
        doNothing().when(subscriptionService).requirePremium(USER_ID);
        subscriptionProvider.requirePremium(USER_ID);
        verify(subscriptionService).requirePremium(USER_ID);

        doThrow(new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED))
                .when(subscriptionService).requirePremium(USER_ID);
        assertThatThrownBy(() -> subscriptionProvider.requirePremium(USER_ID))
                .isInstanceOf(AppException.class);
    }

    @Test
    void hasFeatureDelegatesToSubscriptionService() {
        when(subscriptionService.hasFeature(USER_ID, "ADVANCED_ANALYTICS")).thenReturn(true);
        assertThat(subscriptionProvider.hasFeature(USER_ID, "ADVANCED_ANALYTICS")).isTrue();
        verify(subscriptionService).hasFeature(USER_ID, "ADVANCED_ANALYTICS");

        when(subscriptionService.hasFeature(USER_ID, "INVALID")).thenReturn(false);
        assertThat(subscriptionProvider.hasFeature(USER_ID, "INVALID")).isFalse();
    }
}
