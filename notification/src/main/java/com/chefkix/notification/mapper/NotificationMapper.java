package com.chefkix.notification.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chefkix.notification.entity.Notification;
import com.chefkix.notification.dto.response.NotificationResponse;
import com.chefkix.shared.event.PostLikeEvent;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "isSummary", expression = "java(notification.getCount() > 1)")
    @Mapping(target = "actorInfo.actorId", source = "latestActorId")
    @Mapping(target = "actorInfo.actorName", source = "latestActorName")
    @Mapping(target = "actorInfo.avatarUrl", source = "latestActorAvatarUrl")
    NotificationResponse toNotificationResponse(Notification notification);

    @Mapping(target = "recipientId", source = "postOwnerId")
    @Mapping(target = "latestActorId", source = "likerId")
    @Mapping(target = "count", constant = "1")
    @Mapping(target = "isRead", constant = "false")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Notification toNewNotificationEntity(PostLikeEvent postLikeEvent);
}
