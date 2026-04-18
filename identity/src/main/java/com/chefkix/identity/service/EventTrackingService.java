package com.chefkix.identity.service;

import com.chefkix.identity.dto.request.EventBatchRequest;
import com.chefkix.identity.dto.request.EventItemRequest;
import com.chefkix.identity.entity.UserEvent;
import com.chefkix.identity.repository.UserEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventTrackingService {

    static final String TOPIC = "user-events";

    KafkaTemplate<String, Object> kafkaTemplate;
    UserEventRepository userEventRepository;

    public int trackEvents(String userId, EventBatchRequest request) {
        int accepted = 0;
        for (EventItemRequest item : request.getEvents()) {
            UserEvent event = UserEvent.builder()
                    .userId(userId)
                    .eventType(item.getEventType())
                    .entityId(item.getEntityId())
                    .entityType(item.getEntityType())
                    .metadata(item.getMetadata())
                    .timestamp(item.getTimestamp() != null ? item.getTimestamp() : Instant.now())
                    .build();

            try {
                kafkaTemplate.send(TOPIC, userId, event);
                accepted++;
            } catch (Exception e) {
                log.error("Failed to produce event for user={} type={}: {}",
                        userId, item.getEventType(), e.getMessage());
            }
        }
        log.debug("Accepted {}/{} events for user={}", accepted, request.getEvents().size(), userId);
        return accepted;
    }

    public void storeEvent(UserEvent event) {
        userEventRepository.save(event);
    }

    public void storeEvents(List<UserEvent> events) {
        userEventRepository.saveAll(events);
    }

    public long deleteUserEvents(String userId) {
        long count = userEventRepository.deleteByUserId(userId);
        log.info("Deleted {} events for user={}", count, userId);
        return count;
    }
}
