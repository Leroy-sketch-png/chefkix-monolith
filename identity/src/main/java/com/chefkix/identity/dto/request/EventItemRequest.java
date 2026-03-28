package com.chefkix.identity.dto.request;

import com.chefkix.identity.enums.TrackingEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100) String entityId;

    @Size(max = 50) String entityType;

    @Size(max = 50) Map<String, Object> metadata;

    Instant timestamp;
}
