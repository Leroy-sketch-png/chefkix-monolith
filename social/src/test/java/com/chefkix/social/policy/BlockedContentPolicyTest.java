package com.chefkix.social.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockedContentPolicyTest {

    @Mock private ProfileProvider profileProvider;

    @InjectMocks private BlockedContentPolicy policy;

    @Test
    void returnsDistinctInvisibleAccountsForAuthenticatedViewer() {
        when(profileProvider.getInvisibleUserIds("viewer"))
                .thenReturn(Arrays.asList("blocked", "blocker", "blocked", null));

        assertThat(policy.invisibleUserIds("viewer")).containsExactly("blocked", "blocker");
    }

    @Test
    void doesNotQueryBlockStoreForAnonymousViewer() {
        assertThat(policy.invisibleUserIds(null)).isEmpty();

        verify(profileProvider, never()).getInvisibleUserIds(null);
    }

    @Test
    void failsClosedWhenAuthenticatedBlockLookupReturnsNull() {
        when(profileProvider.getInvisibleUserIds("viewer")).thenReturn(null);

        assertThatThrownBy(() -> policy.invisibleUserIds("viewer"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hidesContentSymmetricallyThroughIdentityPolicy() {
        when(profileProvider.isBlocked("viewer", "author")).thenReturn(true);

        assertThatThrownBy(() -> policy.requireVisible("viewer", "author"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void leavesOwnContentAccessibleWithoutBlockLookup() {
        policy.requireVisible("same-user", "same-user");

        verify(profileProvider, never()).isBlocked("same-user", "same-user");
    }
}
