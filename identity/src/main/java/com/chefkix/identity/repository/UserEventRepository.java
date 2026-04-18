package com.chefkix.identity.repository;

import com.chefkix.identity.entity.UserEvent;
import com.chefkix.identity.enums.TrackingEventType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEventRepository extends MongoRepository<UserEvent, String> {

    List<UserEvent> findByUserIdAndEventTypeAndTimestampBetween(
            String userId, TrackingEventType eventType, Instant from, Instant to);

    long countByUserIdAndEventType(String userId, TrackingEventType eventType);

    List<UserEvent> findByUserIdOrderByTimestampDesc(String userId);

    long countByEventTypeAndTimestampAfter(TrackingEventType eventType, Instant after);

    List<UserEvent> findByUserIdAndEventTypeInOrderByTimestampDesc(
            String userId, List<TrackingEventType> eventTypes);

    long deleteByUserId(String userId);
}
