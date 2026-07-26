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
        deactivateIfExpired(sub);
        return toResponse(sub);
    }

    public boolean isPremium() {
        String userId = getCurrentUserId();
        return isPremium(userId);
    }

    public boolean isPremium(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(this::isPremiumActive)
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
        return isPremium();
    }

    public boolean hasFeature(String userId, PremiumFeature feature) {
        return isPremium(userId);
    }

    /**
     */
    public SubscriptionResponse activateSubscription(String paymentProvider, String paymentToken) {
        String userId = getCurrentUserId();

        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));

        deactivateIfExpired(sub);

        if (sub.isActive() && sub.getTier() == SubscriptionTier.PREMIUM) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        if (paymentToken == null || paymentToken.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Payment token is required");
        }

        throw new AppException(ErrorCode.INVALID_REQUEST,
                "Payment processing is not yet available. Start a free trial instead.");
    }

    /**
     */
    public SubscriptionResponse startTrial() {
        String userId = getCurrentUserId();

        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));

        if (sub.isTrialUsed()) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        deactivateIfExpired(sub);

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
     */
    public SubscriptionResponse cancelSubscription() {
        String userId = getCurrentUserId();

        UserSubscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        deactivateIfExpired(sub);

        if (!sub.isActive() || sub.getTier() != SubscriptionTier.PREMIUM) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        sub.setCancelledAtPeriodEnd(true);
        sub.setCancelledAt(Instant.now());

        log.info("Premium subscription cancelled for user={}, expires={}", userId, sub.getEndDate());
        subscriptionRepository.save(sub);
        return toResponse(sub);
    }

    private void deactivateIfExpired(UserSubscription sub) {
        if (isExpired(sub)) {
            sub.setActive(false);
            sub.setTier(SubscriptionTier.FREE);
            subscriptionRepository.save(sub);
            log.info("Auto-deactivated expired subscription for user={}", sub.getUserId());
        }
    }

    private boolean isPremiumActive(UserSubscription sub) {
        return sub.isActive()
                && sub.getTier() == SubscriptionTier.PREMIUM
                && !isExpired(sub);
    }

    private boolean isExpired(UserSubscription sub) {
        return sub.isActive()
                && sub.getEndDate() != null
                && !Instant.now().isBefore(sub.getEndDate());
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
        boolean premiumActive = isPremiumActive(sub);
        boolean isTrialActive = premiumActive
                && "TRIAL".equals(sub.getPaymentProvider())
                && sub.getTrialEndDate() != null
                && Instant.now().isBefore(sub.getTrialEndDate());

        List<PremiumFeature> features = premiumActive
                ? Arrays.asList(PremiumFeature.values())
                : List.of();

        return SubscriptionResponse.builder()
                .tier(sub.getTier())
                .active(sub.isActive())
                .premium(premiumActive)
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
