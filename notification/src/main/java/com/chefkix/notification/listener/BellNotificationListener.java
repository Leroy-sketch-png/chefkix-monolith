package com.chefkix.notification.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.chefkix.shared.service.KafkaIdempotencyService;
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

    private boolean shouldProcess(BaseEvent event, String topic) {
        if (event == null) {
            log.error("Received null event on topic {}", topic);
            return false;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            log.error("Received event with missing eventId on topic {}", topic);
            return false;
        }
        return idempotencyService.tryProcess(event.getEventId(), topic);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @KafkaListener(
            topics = "post-liked-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenPostLikedDelivery(BaseEvent event) {
        if (!shouldProcess(event, "post-liked-delivery")) {
            return;
        }
        if (event instanceof PostLikeEvent likeEvent) {
            if (!hasText(likeEvent.getPostId()) || !hasText(likeEvent.getPostOwnerId()) || !hasText(likeEvent.getLikerId())) {
                log.error("Skipping invalid PostLikeEvent: postId={}, postOwnerId={}, likerId={}", likeEvent.getPostId(), likeEvent.getPostOwnerId(), likeEvent.getLikerId());
                return;
            }
            try {
                log.info("Received PostLikeEvent for post: {}", likeEvent.getPostId());
                notificationService.handlePostLikeEvent(likeEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "post-liked-delivery");
                throw e;
            }
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "new-follower-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenNewFollowerDelivery(BaseEvent event) {
        if (!shouldProcess(event, "new-follower-delivery")) {
            return;
        }
        if (event instanceof NewFollowerEvent followerEvent) {
            if (!hasText(followerEvent.getFollowerId()) || !hasText(followerEvent.getFollowedUserId())) {
                log.error("Skipping invalid NewFollowerEvent: followerId={}, followedUserId={}", followerEvent.getFollowerId(), followerEvent.getFollowedUserId());
                return;
            }
            try {
                log.info(
                        "Received NewFollowerEvent: {} → {}",
                        followerEvent.getFollowerId(),
                        followerEvent.getFollowedUserId());
                notificationService.handleNewFollowerEvent(followerEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "new-follower-delivery");
                throw e;
            }
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "comment-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenCommentDelivery(BaseEvent event) {
        if (!shouldProcess(event, "comment-delivery")) {
            return;
        }
        if (event instanceof CommentEvent commentEvent) {
            if (!hasText(commentEvent.getPostId()) || !hasText(commentEvent.getPostOwnerId()) || !hasText(commentEvent.getCommenterId())) {
                log.error(
                        "Skipping invalid CommentEvent: postId={}, postOwnerId={}, commenterId={}",
                        commentEvent.getPostId(),
                        commentEvent.getPostOwnerId(),
                        commentEvent.getCommenterId());
                return;
            }
            try {
                log.info(
                        "Received CommentEvent for post: {} from user: {}",
                        commentEvent.getPostId(),
                        commentEvent.getCommenterId());
                notificationService.handleCommentEvent(commentEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "comment-delivery");
                throw e;
            }
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "gamification-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenGamificationDelivery(BaseEvent event) {
        if (!shouldProcess(event, "gamification-delivery")) {
            return;
        }
        if (event instanceof GamificationNotificationEvent gamificationEvent) {
            if (!hasText(gamificationEvent.getUserId())) {
                log.error("Skipping invalid GamificationNotificationEvent: userId is blank");
                return;
            }
            try {
                log.info(
                        "Received GamificationEvent: user={}, leveledUp={}, badges={}",
                        gamificationEvent.getUserId(),
                        gamificationEvent.isLeveledUp(),
                        gamificationEvent.getNewBadges());
                notificationService.handleGamificationEvent(gamificationEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "gamification-delivery");
                throw e;
            }
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "reminder-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenReminderDelivery(BaseEvent event) {
        if (!shouldProcess(event, "reminder-delivery")) {
            return;
        }
        if (event instanceof ReminderEvent reminderEvent) {
            if (!hasText(reminderEvent.getUserId()) || !hasText(reminderEvent.getReminderType())) {
                log.error("Skipping invalid ReminderEvent: userId={}, reminderType={}", reminderEvent.getUserId(), reminderEvent.getReminderType());
                return;
            }
            try {
                log.info(
                        "Received ReminderEvent: user={}, type={}, priority={}",
                        reminderEvent.getUserId(),
                        reminderEvent.getReminderType(),
                        reminderEvent.getPriority());
                notificationService.handleReminderEvent(reminderEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "reminder-delivery");
                throw e;
            }
        } else if (event instanceof DuelEvent duelEvent) {
            if (!hasText(duelEvent.getUserId()) || !hasText(duelEvent.getDuelId())) {
                log.error("Skipping invalid DuelEvent: userId={}, duelId={}", duelEvent.getUserId(), duelEvent.getDuelId());
                return;
            }
            try {
                log.info("Received DuelEvent: user={}, action={}, duel={}",
                        duelEvent.getUserId(), duelEvent.getDuelAction(), duelEvent.getDuelId());
                notificationService.handleDuelEvent(duelEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "reminder-delivery");
                throw e;
            }
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "tag-delivery",
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenTagDelivery(BaseEvent event) {
        if (!shouldProcess(event, "tag-delivery")) {
            return;
        }
        if (event instanceof UserMentionEvent mentionEvent) {
            if (!hasText(mentionEvent.getUserId()) || !hasText(mentionEvent.getSourceType())) {
                log.error("Skipping invalid UserMentionEvent: userId={}, sourceType={}", mentionEvent.getUserId(), mentionEvent.getSourceType());
                return;
            }
            try {
                log.info(
                        "Received UserMentionEvent: recipient={}, actor={}, source={}",
                        mentionEvent.getUserId(),
                        mentionEvent.getActorDisplayName(),
                        mentionEvent.getSourceType());
                notificationService.handleTagEvent(mentionEvent);
            } catch (Exception e) {
                idempotencyService.removeProcessed(event.getEventId(), "tag-delivery");
                throw e;
            }
        } else {
            log.warn("Received unexpected event type on 'tag-delivery': {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(
            topics = "group-delivery", // Ensure your social module produces to this topic!
            groupId = "notification-group",
            containerFactory = "notificationEventListenerFactory")
    public void listenGroupDelivery(BaseEvent event) {

        // 1. Null-safe idempotency check (consistent with all other listeners)
        if (!shouldProcess(event, "group-delivery")) {
            return;
        }

        // 2. Java 21 Pattern Matching Switch to handle the specific group events
        switch (event) {
            case GroupJoinRequestedEvent requestEvent -> {
                if (!hasText(requestEvent.getGroupId()) || !hasText(requestEvent.getRequesterId())) {
                    log.error("Skipping invalid GroupJoinRequestedEvent: groupId={}, requesterId={}", requestEvent.getGroupId(), requestEvent.getRequesterId());
                    return;
                }
                try {
                    log.info("Received GroupJoinRequestedEvent for group: {} from user: {}",
                            requestEvent.getGroupId(), requestEvent.getRequesterId());
                    notificationService.handleGroupJoinRequestedEvent(requestEvent);
                } catch (Exception e) {
                    idempotencyService.removeProcessed(event.getEventId(), "group-delivery");
                    throw e;
                }
            }
            case GroupMemberJoinedEvent joinEvent -> {
                if (!hasText(joinEvent.getGroupId()) || !hasText(joinEvent.getMemberId())) {
                    log.error("Skipping invalid GroupMemberJoinedEvent: groupId={}, memberId={}", joinEvent.getGroupId(), joinEvent.getMemberId());
                    return;
                }
                try {
                    log.info("Received GroupMemberJoinedEvent for group: {} from user: {}",
                            joinEvent.getGroupId(), joinEvent.getMemberId());
                    notificationService.handleGroupMemberJoinedEvent(joinEvent);
                } catch (Exception e) {
                    idempotencyService.removeProcessed(event.getEventId(), "group-delivery");
                    throw e;
                }
            }
            case GroupRequestApprovedEvent approveEvent -> {
                if (!hasText(approveEvent.getGroupId()) || !hasText(approveEvent.getRequesterId())) {
                    log.error("Skipping invalid GroupRequestApprovedEvent: groupId={}, requesterId={}", approveEvent.getGroupId(), approveEvent.getRequesterId());
                    return;
                }
                try {
                    log.info("Received GroupRequestApprovedEvent for group: {} from user: {}",
                            approveEvent.getGroupId(), approveEvent.getRequesterId());
                    notificationService.handleGroupRequestApprovedEvent(approveEvent);
                } catch (Exception e) {
                    idempotencyService.removeProcessed(event.getEventId(), "group-delivery");
                    throw e;
                }
            }
            default -> {
                log.warn("Received unexpected event type on 'group-delivery': {}",
                        event.getClass().getSimpleName());
            }
        }
    }
}
