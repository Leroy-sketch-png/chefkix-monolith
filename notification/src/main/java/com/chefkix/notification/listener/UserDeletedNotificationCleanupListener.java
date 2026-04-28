package com.chefkix.notification.listener;

import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.UserDeletedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeletedNotificationCleanupListener {

    NotificationService notificationService;

    @EventListener
    public void handle(UserDeletedEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("Skipping deleted-user notification cleanup because event userId was blank");
            return;
        }

        long affectedRecords = notificationService.cleanupDeletedUserState(event.getUserId());
        log.info("Cleaned {} notification records for deleted user={}", affectedRecords, event.getUserId());
    }
}