package com.chefkix.shared.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailEvent {

    @Builder.Default
    String eventId = java.util.UUID.randomUUID().toString();

    String recipientEmail;
    String subject;
    String body;
}
