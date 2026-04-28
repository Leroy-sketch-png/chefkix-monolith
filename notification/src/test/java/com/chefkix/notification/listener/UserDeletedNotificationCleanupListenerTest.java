package com.chefkix.notification.listener;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.UserDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeletedNotificationCleanupListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserDeletedNotificationCleanupListener listener;

    @Test
    void handleDelegatesCleanupForDeletedUserEvent() {
        when(notificationService.cleanupDeletedUserState("user-1")).thenReturn(7L);

        listener.handle(UserDeletedEvent.builder().userId("user-1").build());

        verify(notificationService).cleanupDeletedUserState("user-1");
    }

    @Test
    void handleSkipsBlankUserId() {
        listener.handle(UserDeletedEvent.builder().userId(" ").build());

        verify(notificationService, never()).cleanupDeletedUserState(anyString());
    }
}