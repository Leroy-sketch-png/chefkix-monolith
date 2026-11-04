package com.chefkix.identity.service;

import com.chefkix.identity.api.SubscriptionProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionProviderImpl implements SubscriptionProvider {

    SubscriptionService subscriptionService;

    @Override
    public boolean isPremium(String userId) {
        return subscriptionService.isPremium(userId);
    }

    @Override
    public void requirePremium(String userId) {
        subscriptionService.requirePremium(userId);
    }

    @Override
    public boolean hasFeature(String userId, String featureCode) {
        return subscriptionService.hasFeature(userId, featureCode);
    }
}
