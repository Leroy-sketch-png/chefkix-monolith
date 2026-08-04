package com.chefkix.social.post.service;

import com.chefkix.identity.api.dto.BasicProfileInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MentionIdentityTest {

    private final Map<String, BasicProfileInfo> profiles = Map.of(
            "user-1", profile("user-1", "linh.nguyen"),
            "user-2", profile("user-2", "minhtran"));

    @Test
    void keepsOnlyDistinctRecipientsWithVisibleUsernameTokens() {
        List<String> result = MentionIdentity.reconcile(
                "Dinner with @linh.nguyen, not the other person",
                List.of("user-1", "user-1", "user-2"),
                profiles::get);

        assertThat(result).containsExactly("user-1");
    }

    @Test
    void doesNotMatchAUsernamePrefix() {
        List<String> result = MentionIdentity.reconcile(
                "Dinner with @minhtran2 or @minhtran-extra",
                List.of("user-2"),
                profiles::get);

        assertThat(result).isEmpty();
    }

    @Test
    void dropsUnknownBlankAndUnresolvableRecipientsWithoutBlockingContent() {
        List<String> result = MentionIdentity.reconcile(
                "Dinner with @missing",
                List.of("missing"),
                userId -> {
                    throw new IllegalStateException("profile unavailable");
                });

        assertThat(result).isEmpty();
        assertThat(MentionIdentity.reconcile("", List.of("user-1"), profiles::get)).isEmpty();
        assertThat(MentionIdentity.reconcile("hello", null, profiles::get)).isEmpty();
    }

    private static BasicProfileInfo profile(String userId, String username) {
        return BasicProfileInfo.builder()
                .userId(userId)
                .username(username)
                .displayName(username)
                .build();
    }
}
