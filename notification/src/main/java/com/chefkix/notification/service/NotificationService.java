package com.chefkix.notification.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.chefkix.identity.api.NotificationPreferencesProvider;
import com.chefkix.notification.entity.Notification;
import com.chefkix.notification.enums.NotificationType;
import com.chefkix.notification.dto.request.NotificationUpdateRequest;
import com.chefkix.notification.dto.response.NotificationResponse;
import com.chefkix.notification.dto.response.NotificationSummaryResponse;
import com.chefkix.notification.mapper.NotificationMapper;
import com.chefkix.notification.repository.NotificationRepository;
import com.chefkix.shared.event.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    NotificationMapper notificationMapper;
    SimpMessagingTemplate messagingTemplate;
    PushNotificationService pushNotificationService;
    NotificationPreferencesProvider notificationPreferencesProvider;
    MongoTemplate mongoTemplate;

    private static final String USER_TOPIC_PREFIX = "/topic/user/";
    private static final int POST_PREVIEW_LENGTH = 50;

    // ===============================================
    // HELPER METHODS
    // ===============================================

    private String safeDisplayName(String name) {
        return (name != null && !name.isBlank()) ? name : "Someone";
    }

    // ===============================================
    // EVENT HANDLERS
    // ===============================================

    public void handlePostLikeEvent(PostLikeEvent event) {
        String recipientId = event.getPostOwnerId();
        String targetId = event.getPostId();

        // Skip self-like notifications
        if (event.getLikerId().equals(recipientId)) {
            return;
        }

        // PREFERENCE CHECK: respect user's social notification toggle
        if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, "social")) {
            log.debug("Skipping POST_LIKE notification for user {} — social notifications disabled", recipientId);
            return;
        }

        Optional<Notification> existingNotif = notificationRepository.findByRecipientIdAndTargetEntityIdAndType(
                recipientId, targetId, NotificationType.POST_LIKE);

        Notification notificationToSave;

        if (existingNotif.isPresent()) {
            notificationToSave = existingNotif.get();
            updateExistingNotification(notificationToSave, event);
        } else {
            notificationToSave = createNewPostLikeNotification(event);
        }

        notificationRepository.save(notificationToSave);

        NotificationResponse response = notificationMapper.toNotificationResponse(notificationToSave);
        broadcastNotification(recipientId, response, notificationToSave.getCount() == 1 ? "CREATE" : "UPDATE");
    }

    public void handleNewFollowerEvent(NewFollowerEvent event) {
        String recipientId = event.getFollowedUserId();
        String displayName = safeDisplayName(event.getFollowerDisplayName());

        // PREFERENCE CHECK: respect user's follower notification toggle
        if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, "followers")) {
            log.debug("Skipping NEW_FOLLOWER notification for user {} — follower notifications disabled", recipientId);
            return;
        }

        String content = event.isMutualFollow()
                ? String.format("%s followed you back! You're now mutual followers.", displayName)
                : String.format("%s started following you.", displayName);

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(NotificationType.NEW_FOLLOWER)
                .isRead(false)
                .content(content)
                .targetEntityId(event.getFollowerId())
                .latestActorId(event.getFollowerId())
                .latestActorName(displayName)
                .latestActorAvatarUrl(event.getFollowerAvatarUrl())
                .count(1)
                .actorIds(Set.of(event.getFollowerId()))
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toNotificationResponse(notification);
        broadcastNotification(recipientId, response, "CREATE");

        log.info("Created new follower notification for user: {}", recipientId);
    }

    public void handleCommentEvent(CommentEvent event) {
        String recipientId = event.getPostOwnerId();
        String displayName = safeDisplayName(event.getCommenterDisplayName());

        // Skip self-comment notifications
        if (event.getCommenterId().equals(recipientId)) {
            return;
        }

        // PREFERENCE CHECK: respect user's social notification toggle
        if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, "social")) {
            log.debug("Skipping POST_COMMENT notification for user {} — social notifications disabled", recipientId);
            return;
        }

        String content = String.format(
                "%s commented: \"%s\"",
                displayName, event.getContentPreview() != null ? event.getContentPreview() : "");

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(NotificationType.POST_COMMENT)
                .isRead(false)
                .content(content)
                .targetEntityId(event.getPostId())
                .latestActorId(event.getCommenterId())
                .latestActorName(displayName)
                .latestActorAvatarUrl(event.getCommenterAvatarUrl())
                .count(1)
                .actorIds(Set.of(event.getCommenterId()))
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toNotificationResponse(notification);
        broadcastNotification(recipientId, response, "CREATE");

        log.info("Created comment notification for user: {} on post: {}", recipientId, event.getPostId());
    }

    public void handleGamificationEvent(GamificationNotificationEvent event) {
        String recipientId = event.getUserId();
        String displayName = safeDisplayName(event.getDisplayName());

        if (event.isLeveledUp()) {
            // PREFERENCE CHECK: respect user's xpAndLevelUps toggle
            if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, "xpAndLevelUps")) {
                log.debug("Skipping LEVEL_UP notification for user {} — xpAndLevelUps disabled", recipientId);
            } else {
                String levelUpContent = String.format(
                        "Congratulations! You reached Level %d!%s",
                        event.getNewLevel(),
                        event.getNewTitle() != null ? " You're now a " + event.getNewTitle() + "!" : "");

                Notification levelUpNotification = Notification.builder()
                        .recipientId(recipientId)
                        .type(NotificationType.LEVEL_UP)
                        .isRead(false)
                        .content(levelUpContent)
                        .targetEntityId(null)
                        .latestActorId(recipientId)
                        .latestActorName(displayName)
                        .count(1)
                        .actorIds(Set.of(recipientId))
                        .createdAt(Instant.now())
                        .build();

                notificationRepository.save(levelUpNotification);

                NotificationResponse response = notificationMapper.toNotificationResponse(levelUpNotification);
                broadcastNotification(recipientId, response, "CREATE");

                log.info("Created level-up notification for user: {} (level {})", recipientId, event.getNewLevel());
            }
        }

        if (event.getNewBadges() != null && !event.getNewBadges().isEmpty()) {
            // PREFERENCE CHECK: respect user's badges toggle
            if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, "badges")) {
                log.debug("Skipping BADGE_EARNED notification for user {} — badge notifications disabled", recipientId);
            } else {
                String badgeList = String.join(", ", event.getNewBadges());
                String badgeContent = event.getNewBadges().size() == 1
                        ? String.format("You earned a new badge: %s!", badgeList)
                        : String.format(
                                "You earned %d new badges: %s!",
                                event.getNewBadges().size(), badgeList);

                Notification badgeNotification = Notification.builder()
                        .recipientId(recipientId)
                        .type(NotificationType.BADGE_EARNED)
                        .isRead(false)
                        .content(badgeContent)
                        .targetEntityId(null)
                        .latestActorId(recipientId)
                        .latestActorName(displayName)
                        .count(event.getNewBadges().size())
                        .actorIds(Set.of(recipientId))
                        .createdAt(Instant.now())
                        .build();

                notificationRepository.save(badgeNotification);

                NotificationResponse badgeResponse = notificationMapper.toNotificationResponse(badgeNotification);
                broadcastNotification(recipientId, badgeResponse, "CREATE");

                log.info("Created badge notification for user: {} ({})", recipientId, badgeList);
            }
        }
    }

    public void handleReminderEvent(ReminderEvent event) {
        String recipientId = event.getUserId();
        String displayName = safeDisplayName(event.getDisplayName());

        // Shared ReminderEvent uses String reminderType — map to NotificationType enum
        NotificationType reminderType;
        try {
            reminderType = NotificationType.valueOf(event.getReminderType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown reminder type '{}', defaulting to STREAK_WARNING", event.getReminderType());
            reminderType = NotificationType.STREAK_WARNING;
        }

        // PREFERENCE CHECK: map reminder type to preference category
        String prefCategory = switch (reminderType) {
            case STREAK_WARNING -> "streakWarning";
            case POST_DEADLINE -> "postDeadline";
            case CHALLENGE_REMINDER, CHALLENGE_AVAILABLE -> "dailyChallenge";
            case WEEKEND_NUDGE -> "weekendNudge";
            case PANTRY_EXPIRING -> "pantryReminder";
            default -> "social";
        };
        if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, prefCategory)) {
            log.debug("Skipping {} notification for user {} — {} disabled", reminderType, recipientId, prefCategory);
            return;
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(reminderType)
                .isRead(false)
                .content(event.getContent())
                .targetEntityId(event.getSessionId())
                .latestActorId(recipientId)
                .latestActorName(displayName)
                .count(1)
                .actorIds(Set.of(recipientId))
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toNotificationResponse(notification);
        broadcastNotification(recipientId, response, "CREATE");

        log.info("Created {} notification for user: {} - {}", reminderType, recipientId, event.getContent());
    }

    public void handleTagEvent(UserMentionEvent event) {
        String recipientId = event.getUserId();
        String displayName = safeDisplayName(event.getActorDisplayName());

        // PREFERENCE CHECK: mentions fall under social notifications
        if (!notificationPreferencesProvider.isNotificationEnabled(recipientId, "social")) {
            log.debug("Skipping USER_MENTION notification for user {} — social notifications disabled", recipientId);
            return;
        }

        String content;
        String navigationTargetId = event.getPostId();
        String sourceType = event.getSourceType();

        if ("COMMENT".equalsIgnoreCase(sourceType) || "REPLY".equalsIgnoreCase(sourceType)) {
            content = String.format(
                    "%s mentioned you in a comment: \"%s\"", displayName, getPreviewContent(event.getContentPreview()));
        } else {
            content = String.format("%s mentioned you in their post.", displayName);
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(NotificationType.USER_MENTION)
                .isRead(false)
                .content(content)
                .targetEntityId(navigationTargetId)
                .latestActorId(event.getActorId())
                .latestActorName(displayName)
                .latestActorAvatarUrl(event.getActorAvatarUrl())
                .count(1)
                .actorIds(Set.of(event.getActorId()))
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toNotificationResponse(notification);
        broadcastNotification(recipientId, response, "CREATE");

        log.info("Created mention notification for user: {} in source: {}", recipientId, event.getSourceType());
    }

    // ===============================================
    // DUEL EVENT HANDLER
    // ===============================================

    public void handleDuelEvent(DuelEvent event) {
        String recipientId = event.getUserId();

        NotificationType duelType = switch (event.getDuelAction()) {
            case "INVITE" -> NotificationType.DUEL_INVITE;
            case "ACCEPTED" -> NotificationType.DUEL_ACCEPTED;
            case "DECLINED" -> NotificationType.DUEL_DECLINED;
            case "COMPLETED" -> NotificationType.DUEL_COMPLETED;
            case "EXPIRED" -> NotificationType.DUEL_EXPIRED;
            default -> {
                log.warn("Unknown duel action '{}', skipping", event.getDuelAction());
                yield null;
            }
        };

        if (duelType == null) return;

        String content = switch (duelType) {
            case DUEL_INVITE -> String.format("%s challenged you to a cooking duel: %s!",
                    event.getChallengerName(), event.getRecipeTitle());
            case DUEL_ACCEPTED -> String.format("%s accepted your duel challenge: %s!",
                    event.getOpponentName(), event.getRecipeTitle());
            case DUEL_DECLINED -> String.format("%s declined your duel challenge: %s.",
                    event.getOpponentName(), event.getRecipeTitle());
            case DUEL_COMPLETED -> event.getWinnerId() != null
                    ? String.format("Duel complete! Winner: %s (+%dXP) — %s",
                    event.getWinnerId().equals(recipientId) ? "You" : event.getOpponentName(),
                    event.getBonusXp(), event.getRecipeTitle())
                    : String.format("Duel complete: %s — It's a tie!", event.getRecipeTitle());
            case DUEL_EXPIRED -> String.format("Your duel challenge for %s has expired.", event.getRecipeTitle());
            default -> "Cooking duel update";
        };

        createAndBroadcastNotification(
                recipientId,
                event.getDuelId(),
                event.getChallengerName(),
                event.getRecipeCoverUrl(),
                event.getDuelId(),
                content,
                duelType,
                "Created duel notification"
        );
    }

    // ===============================================
    // GROUP EVENT HANDLERS (REFACTORED)
    // ===============================================

    public void handleGroupJoinRequestedEvent(GroupJoinRequestedEvent event) {
        String displayName = safeDisplayName(event.getRequesterDisplayName());
        String content = String.format("%s requested to join your private group: %s",
                displayName, event.getGroupName());

        createAndBroadcastNotification(
                event.getUserId(), // adminId
                event.getRequesterId(),
                displayName,
                event.getRequesterAvatarUrl(),
                event.getGroupId(),
                content,
                NotificationType.JOIN_REQUESTED,
                "Created group join request notification"
        );
    }

    public void handleGroupMemberJoinedEvent(GroupMemberJoinedEvent event) {
        String displayName = safeDisplayName(event.getMemberDisplayName());
        String content = String.format("%s just joined your group: %s!",
                displayName, event.getGroupName());

        createAndBroadcastNotification(
                event.getUserId(), // adminId
                event.getMemberId(),
                displayName,
                event.getMemberAvatarUrl(),
                event.getGroupId(),
                content,
                NotificationType.MEMBER_JOINED,
                "Created new group member notification"
        );
    }

    public void handleGroupRequestApprovedEvent(GroupRequestApprovedEvent event) {

        String content = String.format("Your request to join the group '%s' has been approved!",
                event.getGroupName());

        createAndBroadcastNotification(
                event.getRequesterId(),        // recipient: The user
                event.getGroupId(),            // actorId: Make the GROUP the actor!
                event.getGroupName(),          // actorName: The Group's name
                event.getGroupCoverImageUrl(), // actorAvatar: The Group's cover photo!
                event.getGroupId(),            // targetEntityId: The Group
                content,
                NotificationType.JOIN_REQUEST_APPROVED,
                "Created join request approved notification"
        );
    }

    private void createAndBroadcastNotification(
            String recipientId, String actorId, String actorName, String actorAvatarUrl,
            String targetEntityId, String content, NotificationType type, String logMessagePrefix) {

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .isRead(false)
                .content(content)
                .targetEntityId(targetEntityId)
                .latestActorId(actorId)
                .latestActorName(actorName)
                .latestActorAvatarUrl(actorAvatarUrl)
                .count(1)
                .actorIds(Set.of(actorId))
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toNotificationResponse(notification);
        broadcastNotification(recipientId, response, "CREATE");

        log.info("{} for recipient: {} regarding target: {}", logMessagePrefix, recipientId, targetEntityId);
    }

    // ===============================================
    // INTERNAL HELPERS
    // ===============================================

    private Notification createNewPostLikeNotification(PostLikeEvent event) {
        String displayName = safeDisplayName(event.getDisplayName());

        return Notification.builder()
                .recipientId(event.getPostOwnerId())
                .type(NotificationType.POST_LIKE)
                .isRead(false)
                .content(String.format("%s liked your post: \"%s\"", displayName, event.getContent()))
                .targetEntityId(event.getPostId())
                .latestActorId(event.getLikerId())
                .latestActorName(displayName)
                .latestActorAvatarUrl(event.getLikerAvatarUrl())
                .count(1)
                .actorIds(Set.of(event.getLikerId()))
                .createdAt(Instant.now())
                .build();
    }

    private void updateExistingNotification(Notification notification, PostLikeEvent event) {
        String displayName = safeDisplayName(event.getDisplayName());

        if (!notification.getActorIds().contains(event.getLikerId())) {
            notification.setCount(notification.getCount() + 1);
            notification.getActorIds().add(event.getLikerId());
            notification.setLatestActorId(event.getLikerId());
            notification.setLatestActorName(displayName);
            notification.setLatestActorAvatarUrl(event.getLikerAvatarUrl());
            notification.setIsRead(false);
            notification.setCreatedAt(Instant.now());

            String postPreview = getPreviewContent(event.getContent());
            notification.setContent(String.format(
                    "%s and %d others liked your post: \"%s\"",
                    displayName, notification.getCount() - 1, postPreview));
        }
    }

    // ===============================================
    // READ / UPDATE OPERATIONS
    // ===============================================

    public long getUnreadNotificationCount(String userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    public List<NotificationResponse> getNotifications(String userId, int limit, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository
                    .findAllByRecipientIdAndIsReadFalse(userId, pageable)
                    .getContent();
        } else {
            notifications = notificationRepository
                    .findAllByRecipientId(userId, pageable)
                    .getContent();
        }

        return notifications.stream()
                .map(notificationMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }

    public void updateReadStatus(String userId, NotificationUpdateRequest request) {
        Iterable<Notification> allNotifications = notificationRepository.findAllById(request.getNotificationIds());

        List<Notification> ownedNotifications = new java.util.ArrayList<>();
        for (Notification notification : allNotifications) {
            if (notification.getRecipientId().equals(userId)) {
                notification.setIsRead(request.getIsRead());
                ownedNotifications.add(notification);
            } else {
                log.warn(
                        "User {} attempted to update notification {} which belongs to {}",
                        userId,
                        notification.getId(),
                        notification.getRecipientId());
            }
        }

        if (!ownedNotifications.isEmpty()) {
            notificationRepository.saveAll(ownedNotifications);
        }
    }

    public void markAsRead(String userId, String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getRecipientId().equals(userId)) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            } else {
                log.warn(
                        "User {} attempted to mark notification {} which belongs to {}",
                        userId,
                        notificationId,
                        notification.getRecipientId());
            }
        });
    }

    public long markAllAsRead(String userId) {
        long updated = mongoTemplate.updateMulti(
                Query.query(Criteria.where("recipientId").is(userId).and("isRead").is(false)),
                Update.update("isRead", true),
                Notification.class
        ).getModifiedCount();

        if (updated > 0) {
            log.info("Marked {} notifications as read for user {}", updated, userId);
        }
        return updated;
    }

    // ===============================================
    // WEBSOCKET + PUSH BROADCAST
    // ===============================================

    public void broadcastNotification(String recipientId, NotificationResponse response, String action) {
        // WebSocket broadcast for real-time delivery
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("notification", response);

        messagingTemplate.convertAndSend(USER_TOPIC_PREFIX + recipientId, payload);
        log.info("Broadcasted {} notification to user: {}", action, recipientId);

        // Push notification for background/offline delivery
        if ("CREATE".equals(action)) {
            sendPushNotification(recipientId, response);
        }
    }

    /**
     * Send push notification to user's devices.
     */
    private void sendPushNotification(String recipientId, NotificationResponse response) {
        String title = "ChefKix";
        String body = response.getContent();

        if (response.getType() != null) {
            switch (response.getType()) {
                case POST_LIKE -> title = "❤️ New Like";
                case POST_COMMENT -> title = "💬 New Comment";
                case NEW_FOLLOWER -> title = "👋 New Follower";
                case LEVEL_UP -> title = "🎉 Level Up!";
                case BADGE_EARNED -> title = "🏆 Badge Earned!";
                case STREAK_WARNING -> title = "🔥 Streak Alert";
                case USER_MENTION -> title = "📝 You were mentioned";
                // Added new push notification titles for Groups!
                case JOIN_REQUESTED -> title = "🚪 Group Request";
                case MEMBER_JOINED -> title = "👋 New Group Member";
                case JOIN_REQUEST_APPROVED -> title = "✅ Request Approved";
                default -> title = "ChefKix";
            }
        }

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", response.getId());
        data.put("type", response.getType() != null ? response.getType().name() : "GENERAL");
        if (response.getTargetEntityId() != null) {
            data.put("targetId", response.getTargetEntityId());
        }
        if (response.getTargetEntityUrl() != null) {
            data.put("link", response.getTargetEntityUrl());
        }

        pushNotificationService.sendToUser(recipientId, title, body, data);
    }

    private String getPreviewContent(String content) {
        if (content == null || content.isBlank()) return "";
        return content.substring(0, Math.min(content.length(), POST_PREVIEW_LENGTH))
                + (content.length() > POST_PREVIEW_LENGTH ? "..." : "");
    }

    // ===============================================
    // WELCOME BACK — ACTIVITY SUMMARY
    // ===============================================

    /**
     * Aggregate notifications since a given timestamp, grouped by type.
     * Powers the "Welcome Back" dashboard card.
     */
    public NotificationSummaryResponse getActivitySummary(String userId, Instant since) {
        Instant maxLookback = Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS);
        Instant cappedSince = since.isBefore(maxLookback) ? maxLookback : since;

        List<Notification> notifications = notificationRepository
                .findAllByRecipientIdAndCreatedAtAfter(userId, cappedSince);

        Map<NotificationType, Long> counts = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));

        return NotificationSummaryResponse.builder()
                .newLikes(countTypes(counts, NotificationType.POST_LIKE, NotificationType.RECIPE_LIKED))
                .newFollowers(countTypes(counts, NotificationType.NEW_FOLLOWER, NotificationType.FOLLOW))
                .newComments(countTypes(counts, NotificationType.POST_COMMENT))
                .newMentions(countTypes(counts, NotificationType.USER_MENTION))
                .challengesAvailable(countTypes(counts, NotificationType.CHALLENGE_AVAILABLE, NotificationType.CHALLENGE_REMINDER))
                .xpAwarded(countTypes(counts, NotificationType.XP_AWARDED, NotificationType.CREATOR_BONUS))
                .levelsGained(countTypes(counts, NotificationType.LEVEL_UP))
                .badgesEarned(countTypes(counts, NotificationType.BADGE_EARNED))
                .roomInvites(countTypes(counts, NotificationType.ROOM_INVITE))
                .totalNotifications(notifications.size())
                .since(since)
                .build();
    }

    /**
     * Sum counts for one or more notification types.
     */
    private int countTypes(Map<NotificationType, Long> counts, NotificationType... types) {
        int total = 0;
        for (NotificationType type : types) {
            total += counts.getOrDefault(type, 0L).intValue();
        }
        return total;
    }
}
