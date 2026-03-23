package com.chefkix.identity.service;

import com.chefkix.identity.dto.response.SubscriptionResponse;
import com.chefkix.identity.entity.*;
import com.chefkix.identity.repository.UserSubscriptionRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionService {

    UserSubscriptionRepository subscriptionRepository;

    public SubscriptionResponse getMySubscription() {
        String userId = getCurrentUserId();
        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));
        return toResponse(sub);
    }

    public boolean isPremium() {
        String userId = getCurrentUserId();
        return isPremium(userId);
    }

    public boolean isPremium(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(sub -> sub.isActive() && sub.getTier() == SubscriptionTier.PREMIUM)
                .orElse(false);
    }

    public void requirePremium() {
        if (!isPremium()) {
            throw new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED);
        }
    }

    public void requirePremium(String userId) {
        if (!isPremium(userId)) {
            throw new AppException(ErrorCode.PREMIUM_FEATURE_REQUIRED);
        }
    }

    public boolean hasFeature(PremiumFeature feature) {
        return isPremium(); // All premium features unlocked with subscription
    }

    public boolean hasFeature(String userId, PremiumFeature feature) {
        return isPremium(userId);
    }

    /**
     * Activate a premium subscription.
     * In production, this would validate the payment token with the provider.
     * For now, it's a skeleton that activates the subscription directly.
     */
    public SubscriptionResponse activateSubscription(String paymentProvider, String paymentToken) {
        String userId = getCurrentUserId();

        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));

        if (sub.isActive() && sub.getTier() == SubscriptionTier.PREMIUM) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // TODO: Validate paymentToken with the actual payment provider (Stripe, Google Play, Apple IAP)
        // For skeleton: directly activate

        Instant now = Instant.now();
        sub.setTier(SubscriptionTier.PREMIUM);
        sub.setActive(true);
        sub.setPaymentProvider(paymentProvider);
        sub.setExternalSubscriptionId(paymentToken); // In production, exchange token for subscription ID
        sub.setStartDate(now);
        sub.setEndDate(now.plus(30, ChronoUnit.DAYS)); // 30-day billing cycle
        sub.setCancelledAtPeriodEnd(false);
        sub.setCancelledAt(null);

        log.info("Premium subscription activated for user={}, provider={}", userId, paymentProvider);
        subscriptionRepository.save(sub);
        return toResponse(sub);
    }

    /**
     * Start a free trial (7 days).
     */
    public SubscriptionResponse startTrial() {
        String userId = getCurrentUserId();

        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));

        if (sub.isTrialUsed()) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        if (sub.isActive() && sub.getTier() == SubscriptionTier.PREMIUM) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        Instant now = Instant.now();
        sub.setTier(SubscriptionTier.PREMIUM);
        sub.setActive(true);
        sub.setTrialUsed(true);
        sub.setTrialStartDate(now);
        sub.setTrialEndDate(now.plus(7, ChronoUnit.DAYS));
        sub.setStartDate(now);
        sub.setEndDate(now.plus(7, ChronoUnit.DAYS));
        sub.setPaymentProvider("TRIAL");

        log.info("Premium trial started for user={}", userId);
        subscriptionRepository.save(sub);
        return toResponse(sub);
    }

    /**
     * Cancel subscription at end of period.
     */
    public SubscriptionResponse cancelSubscription() {
        String userId = getCurrentUserId();

        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!sub.isActive() || sub.getTier() != SubscriptionTier.PREMIUM) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        sub.setCancelledAtPeriodEnd(true);
        sub.setCancelledAt(Instant.now());

        log.info("Premium subscription cancelled for user={}, expires={}", userId, sub.getEndDate());
        subscriptionRepository.save(sub);
        return toResponse(sub);
    }

    private UserSubscription createFreeSubscription(String userId) {
        UserSubscription sub = UserSubscription.builder()
                .userId(userId)
                .tier(SubscriptionTier.FREE)
                .active(false)
                .build();
        return subscriptionRepository.save(sub);
    }

    private SubscriptionResponse toResponse(UserSubscription sub) {
        boolean isPremiumActive = sub.isActive() && sub.getTier() == SubscriptionTier.PREMIUM;
        boolean isTrialActive = isPremiumActive
                && "TRIAL".equals(sub.getPaymentProvider())
                && sub.getTrialEndDate() != null
                && Instant.now().isBefore(sub.getTrialEndDate());

        List<PremiumFeature> features = isPremiumActive
                ? Arrays.asList(PremiumFeature.values())
                : List.of();

        return SubscriptionResponse.builder()
                .tier(sub.getTier())
                .active(sub.isActive())
                .premium(isPremiumActive)
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .trialUsed(sub.isTrialUsed())
                .trialActive(isTrialActive)
                .cancelledAtPeriodEnd(sub.isCancelledAtPeriodEnd())
                .cancelledAt(sub.getCancelledAt())
                .availableFeatures(features)
                .createdAt(sub.getCreatedAt())
                .build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
