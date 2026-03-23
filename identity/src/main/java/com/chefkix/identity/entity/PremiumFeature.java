package com.chefkix.identity.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PremiumFeature {
    AD_FREE("Ad Free"),
    PREMIUM_BADGES("Premium Badges"),
    CUSTOM_PROFILE_THEMES("Custom Profile Themes"),
    CUSTOM_TIMER_SOUNDS("Custom Timer Sounds"),
    ADVANCED_ANALYTICS("Advanced Analytics"),
    PRIORITY_SUPPORT("Priority Support"),
    UNLIMITED_SAVES("Unlimited Saves"),
    EARLY_ACCESS("Early Access Features"),
    EXCLUSIVE_CHALLENGES("Exclusive Challenges"),
    PREMIUM_COSMETICS("Premium Cosmetics");

    private final String displayName;

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
