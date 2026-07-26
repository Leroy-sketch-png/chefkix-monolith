package com.chefkix.culinary.features.challenge.service;

import java.time.Instant;
import java.util.Objects;

public final class ChallengeLifecyclePolicy {

    public enum WindowStatus {
        UPCOMING,
        ACTIVE,
        ENDED
    }

    private ChallengeLifecyclePolicy() {
    }

    public static WindowStatus statusAt(Instant startsAt, Instant endsAt, Instant now) {
        Objects.requireNonNull(now, "now");

        if (endsAt == null || !endsAt.isAfter(now)) {
            return WindowStatus.ENDED;
        }
        if (startsAt != null && startsAt.isAfter(now)) {
            return WindowStatus.UPCOMING;
        }
        return WindowStatus.ACTIVE;
    }

    public static boolean isActive(Instant startsAt, Instant endsAt, Instant now) {
        return statusAt(startsAt, endsAt, now) == WindowStatus.ACTIVE;
    }
}
