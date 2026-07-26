package com.chefkix.social.post.policy;

import java.time.Duration;
import java.time.Instant;

public final class TrendingPolicy {

    public static final Duration ELIGIBILITY_WINDOW = Duration.ofDays(7);

    private TrendingPolicy() {
    }

    public static Instant cutoff(Instant now) {
        return now.minus(ELIGIBILITY_WINDOW);
    }
}
