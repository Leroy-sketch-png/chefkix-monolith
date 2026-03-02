package com.chefkix.notification.dto.response;

import java.time.Instant;

import com.chefkix.notification.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {

    private String id;

    private NotificationType type;

    @JsonProperty("isRead")
    @Builder.Default
    private Boolean isRead = false;

    private String content;
    private String targetEntityId;
    private String targetEntityUrl;
    private Instant createdAt;

    private int count;
    private String latestActorId;
    private String latestActorName;
    private String latestActorAvatarUrl;

    @JsonProperty("isSummary")
    @Builder.Default
    private Boolean isSummary = false;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ActorInfo {
        private String actorId;
        private String actorName;
        private String avatarUrl;
    }

    private ActorInfo actorInfo;
}
