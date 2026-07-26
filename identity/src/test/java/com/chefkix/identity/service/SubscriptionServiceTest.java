package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.dto.response.SubscriptionResponse;
import com.chefkix.identity.entity.PremiumFeature;
import com.chefkix.identity.entity.SubscriptionTier;
import com.chefkix.identity.entity.UserSubscription;
import com.chefkix.identity.repository.UserSubscriptionRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final String USER_ID = "cook-1";

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accountStatusExpiresStalePremiumAndMatchesEntitlementStatus() {
        UserSubscription subscription = premiumSubscription(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(same(subscription))).thenReturn(subscription);

        SubscriptionResponse response = subscriptionService.getMySubscription();

        assertThat(response.isPremium()).isFalse();
        assertThat(response.isActive()).isFalse();
        assertThat(response.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(response.isTrialActive()).isFalse();
        assertThat(response.isTrialUsed()).isTrue();
        assertThat(response.getAvailableFeatures()).isEmpty();
        assertThat(subscriptionService.isPremium(USER_ID)).isFalse();
        verify(subscriptionRepository).save(same(subscription));
    }

    @Test
    void futurePremiumRemainsActiveAcrossAccountAndEntitlementStatus() {
        UserSubscription subscription = premiumSubscription(Instant.now().plus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getMySubscription();

        assertThat(response.isPremium()).isTrue();
        assertThat(response.isActive()).isTrue();
        assertThat(response.getTier()).isEqualTo(SubscriptionTier.PREMIUM);
        assertThat(response.isTrialActive()).isTrue();
        assertThat(response.getAvailableFeatures())
                .containsExactlyInAnyOrder(Arrays.stream(PremiumFeature.values()).toArray(PremiumFeature[]::new));
        assertThat(subscriptionService.isPremium(USER_ID)).isTrue();
        verify(subscriptionRepository, never()).save(subscription);
    }

    @Test
    void cancellationKeepsPremiumActiveUntilTheDeclaredEndDate() {
        UserSubscription subscription = premiumSubscription(Instant.now().plus(1, ChronoUnit.DAYS));
        subscription.setCancelledAtPeriodEnd(true);
        subscription.setCancelledAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(subscriptionRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getMySubscription();

        assertThat(response.isPremium()).isTrue();
        assertThat(response.isActive()).isTrue();
        assertThat(response.isCancelledAtPeriodEnd()).isTrue();
        assertThat(subscriptionService.isPremium(USER_ID)).isTrue();
        verify(subscriptionRepository, never()).save(subscription);
    }

    @Test
    void cancellationNormalizesExpiredPremiumBeforeRejectingTheAction() {
        UserSubscription subscription = premiumSubscription(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(same(subscription))).thenReturn(subscription);

        assertThatThrownBy(subscriptionService::cancelSubscription)
                .isInstanceOfSatisfying(
                        AppException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        assertThat(subscription.isActive()).isFalse();
        assertThat(subscription.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(subscription.isCancelledAtPeriodEnd()).isFalse();
        verify(subscriptionRepository).save(same(subscription));
    }

    private UserSubscription premiumSubscription(Instant endDate) {
        return UserSubscription.builder()
                .userId(USER_ID)
                .tier(SubscriptionTier.PREMIUM)
                .active(true)
                .trialUsed(true)
                .trialStartDate(Instant.now().minus(6, ChronoUnit.DAYS))
                .trialEndDate(endDate)
                .startDate(Instant.now().minus(6, ChronoUnit.DAYS))
                .endDate(endDate)
                .paymentProvider("TRIAL")
                .build();
    }
}
