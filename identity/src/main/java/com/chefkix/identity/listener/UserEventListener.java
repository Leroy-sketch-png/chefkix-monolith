package com.chefkix.identity.listener;

import com.chefkix.identity.entity.UserEvent;
import com.chefkix.identity.service.EventTrackingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserEventListener {

    EventTrackingService eventTrackingService;

    @KafkaListener(
            topics = "user-events",
            groupId = "user-events-group",
            containerFactory = "userEventKafkaListenerContainerFactory")
    public void handleUserEvent(UserEvent event) {
        if (event == null || event.getUserId() == null || event.getEventType() == null) {
            log.warn("Received invalid user event, skipping");
            return;
        }

        try {
            eventTrackingService.storeEvent(event);
            log.trace("Stored event type={} for user={}", event.getEventType(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to store user event: userId={}, type={}, error={}",
                    event.getUserId(), event.getEventType(), e.getMessage());
        }
    }
}
