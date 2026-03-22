package com.chefkix.identity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventBatchRequest {

    @NotEmpty(message = "Events list cannot be empty")
    @Size(max = 100, message = "Maximum 100 events per batch")
    @Valid
    List<EventItemRequest> events;
}
