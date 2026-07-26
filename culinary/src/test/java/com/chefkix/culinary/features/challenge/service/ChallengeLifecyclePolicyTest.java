package com.chefkix.culinary.features.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChallengeLifecyclePolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    @Test
    void startIsInclusiveAndEndIsExclusive() {
        assertThat(ChallengeLifecyclePolicy.statusAt(NOW, NOW.plusSeconds(60), NOW))
                .isEqualTo(ChallengeLifecyclePolicy.WindowStatus.ACTIVE);
        assertThat(ChallengeLifecyclePolicy.statusAt(NOW.minusSeconds(60), NOW, NOW))
                .isEqualTo(ChallengeLifecyclePolicy.WindowStatus.ENDED);
    }

    @Test
    void distinguishesUpcomingActiveAndEndedWindows() {
        assertThat(ChallengeLifecyclePolicy.statusAt(NOW.plusSeconds(1), NOW.plusSeconds(60), NOW))
                .isEqualTo(ChallengeLifecyclePolicy.WindowStatus.UPCOMING);
        assertThat(ChallengeLifecyclePolicy.statusAt(NOW.minusSeconds(1), NOW.plusSeconds(60), NOW))
                .isEqualTo(ChallengeLifecyclePolicy.WindowStatus.ACTIVE);
        assertThat(ChallengeLifecyclePolicy.statusAt(NOW.minusSeconds(60), NOW.minusSeconds(1), NOW))
                .isEqualTo(ChallengeLifecyclePolicy.WindowStatus.ENDED);
    }

    @Test
    void missingEndTimeFailsClosed() {
        assertThat(ChallengeLifecyclePolicy.statusAt(NOW.minusSeconds(60), null, NOW))
                .isEqualTo(ChallengeLifecyclePolicy.WindowStatus.ENDED);
        assertThat(ChallengeLifecyclePolicy.isActive(null, null, NOW)).isFalse();
    }
}
