package com.chefkix.identity.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionTier {
    FREE("Free"),
    PREMIUM("Premium");

    private final String displayName;

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
