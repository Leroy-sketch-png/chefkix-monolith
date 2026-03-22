package com.chefkix.identity.dto.request;

import com.chefkix.identity.enums.TrackingEventType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventItemRequest {

    @NotNull(message = "Event type is required")
    TrackingEventType eventType;

    String entityId;

    String entityType;

    Map<String, Object> metadata;

    Instant timestamp;
}
