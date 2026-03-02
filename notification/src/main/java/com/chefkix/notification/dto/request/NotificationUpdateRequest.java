package com.chefkix.notification.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationUpdateRequest {

    @NotNull(message = "Notification IDs must not be null")
    private Set<String> notificationIds;

    @NotNull(message = "Read status must be specified")
    private Boolean isRead;
}
