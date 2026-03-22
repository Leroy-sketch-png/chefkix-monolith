package com.chefkix.identity.entity;

import com.chefkix.identity.enums.TrackingEventType;
import java.time.Instant;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "user_events")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_type", def = "{'userId': 1, 'eventType': 1}"),
    @CompoundIndex(name = "idx_user_timestamp", def = "{'userId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_entity", def = "{'entityId': 1, 'eventType': 1}")
})
public class UserEvent {

    @Id
    String id;

    @Indexed
    String userId;

    TrackingEventType eventType;

    String entityId;

    String entityType;

    Map<String, Object> metadata;

    Instant timestamp;

    @CreatedDate
    @Indexed(expireAfter = "90d")
    Instant createdAt;
}
