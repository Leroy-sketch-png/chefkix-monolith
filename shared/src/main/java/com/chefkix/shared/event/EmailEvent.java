package com.chefkix.shared.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Standalone email event (does NOT extend {@link BaseEvent}).
 * <p>
 * Produced by identity module, consumed by notification module.
 * Used for OTP codes, welcome emails, and transactional emails.
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
