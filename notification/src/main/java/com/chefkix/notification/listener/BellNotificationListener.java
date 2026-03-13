package com.chefkix.notification.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.chefkix.config.KafkaIdempotencyService;
import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka listener for bell notification events.
 * Consumes polymorphic BaseEvent messages and delegates to NotificationService.
 * All methods are idempotent via Redis-based event deduplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BellNotificationListener {

    private final NotificationService notificationService;
    private final KafkaIdempotencyService idempotencyService;

    @KafkaListener(
            topics = "post-liked-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenPostLikedDelivery(BaseEvent event) {
        if (!idempotencyService.tryProcess(event.getEventId(), "post-liked-delivery")) {
            return;
        }
        if (event instanceof PostLikeEvent likeEvent) {
            log.info("Received PostLikeEvent for post: {}", likeEvent.getPostId());
            notificationService.handlePostLikeEvent(likeEvent);
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "new-follower-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenNewFollowerDelivery(BaseEvent event) {
        if (!idempotencyService.tryProcess(event.getEventId(), "new-follower-delivery")) {
            return;
        }
        if (event instanceof NewFollowerEvent followerEvent) {
            log.info(
                    "Received NewFollowerEvent: {} → {}",
                    followerEvent.getFollowerId(),
                    followerEvent.getFollowedUserId());
            notificationService.handleNewFollowerEvent(followerEvent);
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "comment-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenCommentDelivery(BaseEvent event) {
        if (!idempotencyService.tryProcess(event.getEventId(), "comment-delivery")) {
            return;
        }
        if (event instanceof CommentEvent commentEvent) {
            log.info(
                    "Received CommentEvent for post: {} from user: {}",
                    commentEvent.getPostId(),
                    commentEvent.getCommenterId());
            notificationService.handleCommentEvent(commentEvent);
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "gamification-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenGamificationDelivery(BaseEvent event) {
        if (!idempotencyService.tryProcess(event.getEventId(), "gamification-delivery")) {
            return;
        }
        if (event instanceof GamificationNotificationEvent gamificationEvent) {
            log.info(
                    "Received GamificationEvent: user={}, leveledUp={}, badges={}",
                    gamificationEvent.getUserId(),
                    gamificationEvent.isLeveledUp(),
                    gamificationEvent.getNewBadges());
            notificationService.handleGamificationEvent(gamificationEvent);
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "reminder-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenReminderDelivery(BaseEvent event) {
        if (!idempotencyService.tryProcess(event.getEventId(), "reminder-delivery")) {
            return;
        }
        if (event instanceof ReminderEvent reminderEvent) {
            log.info(
                    "Received ReminderEvent: user={}, type={}, priority={}",
                    reminderEvent.getUserId(),
                    reminderEvent.getReminderType(),
                    reminderEvent.getPriority());
            notificationService.handleReminderEvent(reminderEvent);
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "tag-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenTagDelivery(BaseEvent event) {
        if (!idempotencyService.tryProcess(event.getEventId(), "tag-delivery")) {
            return;
        }
        if (event instanceof UserMentionEvent mentionEvent) {
            log.info(
                    "Received UserMentionEvent: recipient={}, actor={}, source={}",
                    mentionEvent.getUserId(),
                    mentionEvent.getActorDisplayName(),
                    mentionEvent.getSourceType());
            notificationService.handleTagEvent(mentionEvent);
        } else {
            log.warn("Received unexpected event type on 'tag-delivery': {}", event.getClass().getSimpleName());
        }
    }
}
