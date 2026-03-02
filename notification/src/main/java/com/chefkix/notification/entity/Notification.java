package com.chefkix.notification.entity;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.chefkix.notification.enums.NotificationType;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@CompoundIndex(name = "user_read_idx", def = "{'recipientId': 1, 'isRead': 1, 'createdAt': -1}")
public class Notification {

    @Id
    private String id;

    private String recipientId;
    private NotificationType type;

    @Builder.Default
    private Boolean isRead = false;

    private String content;
    private String targetEntityId;
    private String targetEntityUrl;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private int count = 1;
    private String latestActorId;
    private String latestActorName;
    private String latestActorAvatarUrl;
    private Set<String> actorIds;
}
